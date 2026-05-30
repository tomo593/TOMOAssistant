package com.tomo.tomoassistant.controller;

import com.tomo.tomoassistant.common.result.Result;
import com.tomo.tomoassistant.model.dto.ConversationCreateRequest;
import com.tomo.tomoassistant.model.entity.ConversationEntity;
import com.tomo.tomoassistant.model.entity.MessageEntity;
import com.tomo.tomoassistant.model.vo.ConversationVO;
import com.tomo.tomoassistant.model.vo.MessageVO;
import com.tomo.tomoassistant.repository.ConversationRepository;
import com.tomo.tomoassistant.repository.MessageRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @PostMapping
    public Result<ConversationVO> create(@RequestBody @Valid ConversationCreateRequest request) {
        ConversationEntity entity = new ConversationEntity();
        entity.setTitle(request.getTitle());
        entity.setKnowledgeBaseId(request.getKnowledgeBaseId());
        entity.setModelName(request.getModelName());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setLastMessageAt(LocalDateTime.now());
        entity = conversationRepository.save(entity);
        return Result.ok(toVO(entity));
    }

    @GetMapping
    public Result<List<ConversationVO>> list(
            @RequestParam(required = false) Long knowledgeBaseId) {
        List<ConversationVO> list;
        if (knowledgeBaseId != null) {
            list = conversationRepository.findByKnowledgeBaseIdAndDeletedFalseOrderByIdDesc(knowledgeBaseId)
                    .stream().map(this::toVO).toList();
        } else {
            list = conversationRepository.findByDeletedFalseOrderByLastMessageAtDesc()
                    .stream().map(this::toVO).toList();
        }
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<ConversationVO> getById(@PathVariable Long id) {
        return conversationRepository.findById(id)
                .map(entity -> Result.ok(toVO(entity)))
                .orElse(Result.fail(404, "Conversation not found"));
    }

    @GetMapping("/{id}/messages")
    public Result<List<MessageVO>> getMessages(@PathVariable Long id) {
        List<MessageVO> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id)
                .stream().map(this::toMessageVO).toList();
        return Result.ok(messages);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return conversationRepository.findById(id)
                .map(entity -> {
                    conversationRepository.delete(entity);
                    return Result.<Void>ok();
                })
                .orElse(Result.fail(404, "Conversation not found"));
    }

    @PutMapping("/{id}")
    public Result<ConversationVO> update(@PathVariable Long id,
                                         @RequestBody @Valid ConversationCreateRequest request) {
        return conversationRepository.findById(id)
                .map(entity -> {
                    entity.setTitle(request.getTitle());
                    entity.setKnowledgeBaseId(request.getKnowledgeBaseId());
                    entity = conversationRepository.save(entity);
                    return Result.ok(toVO(entity));
                })
                .orElse(Result.fail(404, "Conversation not found"));
    }

    private ConversationVO toVO(ConversationEntity entity) {
        return ConversationVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .messageCount(entity.getMessageCount())
                .lastMessageAt(entity.getLastMessageAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private MessageVO toMessageVO(MessageEntity entity) {
        MessageVO.MessageVOBuilder builder = MessageVO.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt());

        if (entity.getMetadataJson() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(entity.getMetadataJson());
                if (root.has("imageData")) {
                    builder.imageData(root.get("imageData").asText());
                }
                if (root.has("imageName")) {
                    builder.imageName(root.get("imageName").asText());
                }
            } catch (Exception ignored) {
            }
        }

        return builder.build();
    }
}
