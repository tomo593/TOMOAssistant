package com.tomo.tomoassistant.service.document;

import com.tomo.tomoassistant.model.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    List<DocumentVO> uploadDocuments(Long knowledgeBaseId, List<MultipartFile> files);
    void processDocument(Long documentId);
    void deleteDocument(Long documentId);
    List<DocumentVO> getDocuments(Long knowledgeBaseId);
    void reprocessDocument(Long documentId);
}
