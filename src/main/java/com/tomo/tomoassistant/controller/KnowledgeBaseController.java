package com.tomo.tomoassistant.controller;

import com.tomo.tomoassistant.common.result.Result;
import com.tomo.tomoassistant.model.dto.KnowledgeBaseCreateRequest;
import com.tomo.tomoassistant.model.entity.KnowledgeBaseEntity;
import com.tomo.tomoassistant.model.vo.KnowledgeBaseVO;
import com.tomo.tomoassistant.repository.KnowledgeBaseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @PostMapping
    public Result<KnowledgeBaseVO> create(@RequestBody @Valid KnowledgeBaseCreateRequest request) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setEmbeddingModel(request.getEmbeddingModel());
        entity.setChunkSize(request.getChunkSize());
        entity.setChunkOverlap(request.getChunkOverlap());
        entity = knowledgeBaseRepository.save(entity);
        return Result.ok(toVO(entity));
    }

    @GetMapping
    public Result<List<KnowledgeBaseVO>> list() {
        List<KnowledgeBaseVO> list = knowledgeBaseRepository.findByDeletedFalseOrderByIdDesc()
                .stream().map(this::toVO).toList();
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getById(@PathVariable Long id) {
        return knowledgeBaseRepository.findById(id)
                .map(entity -> Result.ok(toVO(entity)))
                .orElse(Result.fail(404, "Knowledge base not found"));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> update(@PathVariable Long id,
                                          @RequestBody @Valid KnowledgeBaseCreateRequest request) {
        return knowledgeBaseRepository.findById(id)
                .map(entity -> {
                    entity.setName(request.getName());
                    entity.setDescription(request.getDescription());
                    entity.setChunkSize(request.getChunkSize());
                    entity.setChunkOverlap(request.getChunkOverlap());
                    entity = knowledgeBaseRepository.save(entity);
                    return Result.ok(toVO(entity));
                })
                .orElse(Result.fail(404, "Knowledge base not found"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return knowledgeBaseRepository.findById(id)
                .map(entity -> {
                    knowledgeBaseRepository.delete(entity);
                    return Result.<Void>ok();
                })
                .orElse(Result.fail(404, "Knowledge base not found"));
    }

    private KnowledgeBaseVO toVO(KnowledgeBaseEntity entity) {
        return KnowledgeBaseVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .embeddingModel(entity.getEmbeddingModel())
                .docCount(entity.getDocCount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
