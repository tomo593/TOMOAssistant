package com.tomo.tomoassistant.service.document;

import com.tomo.tomoassistant.config.properties.RagProperties;
import com.tomo.tomoassistant.model.entity.DocumentEntity;
import com.tomo.tomoassistant.model.entity.KnowledgeBaseEntity;
import com.tomo.tomoassistant.model.enums.DocumentStatus;
import com.tomo.tomoassistant.model.enums.DocumentType;
import com.tomo.tomoassistant.model.vo.DocumentVO;
import com.tomo.tomoassistant.repository.DocumentRepository;
import com.tomo.tomoassistant.repository.KnowledgeBaseRepository;
import com.tomo.tomoassistant.service.document.DocumentChunker.TextChunk;
import com.tomo.tomoassistant.service.document.DocumentParser.DocumentSegment;
import com.tomo.tomoassistant.service.embedding.EmbeddingService;
import com.tomo.tomoassistant.service.embedding.EmbeddingService.EmbeddedChunk;
import com.tomo.tomoassistant.service.multimodal.ImageDescriptionService;
import com.tomo.tomoassistant.service.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final MinioStorageService storageService;
    private final DocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final ImageDescriptionService imageDescriptionService;
    private final RagProperties ragProperties;

    @Override
    @Transactional
    public List<DocumentVO> uploadDocuments(Long knowledgeBaseId, List<MultipartFile> files) {
        KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found: " + knowledgeBaseId));

        List<DocumentVO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            DocumentType docType = DocumentType.fromFileName(file.getOriginalFilename());
            String objectKey = storageService.generateObjectKey(knowledgeBaseId, file.getOriginalFilename());

            // Upload to MinIO
            storageService.uploadFile(file, objectKey);

            // Create document entity
            DocumentEntity doc = new DocumentEntity();
            doc.setKnowledgeBaseId(knowledgeBaseId);
            doc.setFileName(objectKey);
            doc.setOriginalName(file.getOriginalFilename());
            doc.setFileType(docType.name());
            doc.setFileSize(file.getSize());
            doc.setMinioKey(objectKey);
            doc.setStatus(DocumentStatus.PENDING.getCode());
            doc = documentRepository.save(doc);

            // Update KB doc count
            kb.setDocCount(kb.getDocCount() + 1);
            knowledgeBaseRepository.save(kb);

            // Trigger async processing
            processDocument(doc.getId());

            results.add(toVO(doc));
        }
        return results;
    }

    @Async("documentProcessingExecutor")
    @Override
    @Transactional
    public void processDocument(Long documentId) {
        DocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            doc.setStatus(DocumentStatus.PROCESSING.getCode());
            documentRepository.save(doc);

            DocumentType docType = DocumentType.fromFileName(doc.getOriginalName());
            byte[] fileData = storageService.downloadFile(doc.getMinioKey());

            if (docType.isImage()) {
                // Image processing: generate description -> embed
                String description = imageDescriptionService.generateDescription(fileData, doc.getOriginalName());
                doc.setImageDescription(description);

                float[] vector = embeddingService.embedQuery(description);
                Map<String, Object> metadata = Map.of(
                        "is_image", true,
                        "original_name", doc.getOriginalName(),
                        "file_type", docType.name()
                );
                EmbeddedChunk chunk = new EmbeddedChunk(description, vector, metadata, doc.getId().toString(), 0L);
                embeddingService.storeEmbeddings(doc.getKnowledgeBaseId(), List.of(chunk));
                doc.setChunkCount(1);
            } else {
                // Document processing: parse -> chunk -> embed
                List<DocumentSegment> segments = documentParser.parse(
                        new java.io.ByteArrayInputStream(fileData), doc.getOriginalName());

                KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(doc.getKnowledgeBaseId()).orElseThrow();
                List<TextChunk> chunks = documentChunker.chunk(segments, kb.getChunkSize(), kb.getChunkOverlap());

                if (chunks.isEmpty()) {
                    doc.setStatus(DocumentStatus.COMPLETED.getCode());
                    doc.setChunkCount(0);
                    documentRepository.save(doc);
                    return;
                }

                // Batch embed
                List<String> texts = chunks.stream().map(TextChunk::content).toList();
                List<float[]> vectors = embeddingService.embed(texts);

                // Build embedded chunks
                List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    TextChunk chunk = chunks.get(i);
                    Map<String, Object> metadata = new HashMap<>(chunk.metadata());
                    metadata.put("original_name", doc.getOriginalName());
                    metadata.put("file_type", docType.name());
                    embeddedChunks.add(new EmbeddedChunk(
                            chunk.content(), vectors.get(i), metadata, doc.getId().toString(), (long) i));
                }

                embeddingService.storeEmbeddings(doc.getKnowledgeBaseId(), embeddedChunks);
                doc.setChunkCount(chunks.size());
            }

            doc.setStatus(DocumentStatus.COMPLETED.getCode());
            documentRepository.save(doc);
            log.info("Document processed successfully: {}", doc.getOriginalName());

        } catch (Exception e) {
            log.error("Failed to process document: {}", doc.getOriginalName(), e);
            doc.setStatus(DocumentStatus.FAILED.getCode());
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        DocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // Delete from Milvus
        try {
            embeddingService.deleteEmbeddings(doc.getKnowledgeBaseId(), doc.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to delete embeddings: {}", e.getMessage());
        }

        // Delete from MinIO
        try {
            storageService.deleteFile(doc.getMinioKey());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", e.getMessage());
        }

        documentRepository.delete(doc);

        // Update KB doc count
        knowledgeBaseRepository.findById(doc.getKnowledgeBaseId()).ifPresent(kb -> {
            kb.setDocCount(Math.max(0, kb.getDocCount() - 1));
            knowledgeBaseRepository.save(kb);
        });
    }

    @Override
    public List<DocumentVO> getDocuments(Long knowledgeBaseId) {
        return documentRepository.findByKnowledgeBaseIdAndDeletedFalseOrderByIdDesc(knowledgeBaseId)
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void reprocessDocument(Long documentId) {
        DocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // Delete old embeddings from Milvus before reprocessing
        try {
            embeddingService.deleteEmbeddings(doc.getKnowledgeBaseId(), doc.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to delete old embeddings before reprocessing: {}", e.getMessage());
        }

        doc.setStatus(DocumentStatus.PENDING.getCode());
        doc.setErrorMessage(null);
        documentRepository.save(doc);
        processDocument(documentId);
    }

    private DocumentVO toVO(DocumentEntity entity) {
        return DocumentVO.builder()
                .id(entity.getId())
                .fileName(entity.getOriginalName())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .status(entity.getStatus())
                .chunkCount(entity.getChunkCount())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
