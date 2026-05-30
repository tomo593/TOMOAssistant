package com.tomo.tomoassistant.controller;

import com.tomo.tomoassistant.common.result.Result;
import com.tomo.tomoassistant.model.vo.DocumentVO;
import com.tomo.tomoassistant.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{kbId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public Result<List<DocumentVO>> upload(@PathVariable Long kbId,
                                           @RequestParam("files") MultipartFile[] files) {
        List<MultipartFile> fileList = Arrays.asList(files);
        return Result.ok(documentService.uploadDocuments(kbId, fileList));
    }

    @GetMapping
    public Result<List<DocumentVO>> list(@PathVariable Long kbId) {
        return Result.ok(documentService.getDocuments(kbId));
    }

    @DeleteMapping("/{docId}")
    public Result<Void> delete(@PathVariable Long kbId, @PathVariable Long docId) {
        documentService.deleteDocument(docId);
        return Result.ok();
    }

    @PostMapping("/{docId}/reprocess")
    public Result<Void> reprocess(@PathVariable Long kbId, @PathVariable Long docId) {
        documentService.reprocessDocument(docId);
        return Result.ok();
    }
}
