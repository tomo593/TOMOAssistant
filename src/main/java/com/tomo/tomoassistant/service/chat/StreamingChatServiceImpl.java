package com.tomo.tomoassistant.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tomo.tomoassistant.config.properties.RagProperties;
import com.tomo.tomoassistant.model.dto.ChatRequest;
import com.tomo.tomoassistant.model.entity.ConversationEntity;
import com.tomo.tomoassistant.model.enums.MessageRole;
import com.tomo.tomoassistant.model.enums.StreamingEventType;
import com.tomo.tomoassistant.model.vo.CitationVO;
import com.tomo.tomoassistant.model.vo.StreamingEvent;
import com.tomo.tomoassistant.repository.ConversationRepository;
import com.tomo.tomoassistant.service.memory.ConversationMemoryService;
import com.tomo.tomoassistant.service.retrieval.RetrievalService;
import com.tomo.tomoassistant.service.retrieval.RetrievalService.RetrievalResult;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import com.tomo.tomoassistant.config.LlmModelHolder;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class StreamingChatServiceImpl implements StreamingChatService {

    private final LlmModelHolder modelHolder;
    private final ConversationMemoryService memoryService;
    private final RetrievalService retrievalService;
    private final ConversationRepository conversationRepository;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    public StreamingChatServiceImpl(
            LlmModelHolder modelHolder,
            ConversationMemoryService memoryService,
            RetrievalService retrievalService,
            ConversationRepository conversationRepository,
            RagProperties ragProperties,
            ObjectMapper objectMapper) {
        this.modelHolder = modelHolder;
        this.memoryService = memoryService;
        this.retrievalService = retrievalService;
        this.conversationRepository = conversationRepository;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    private static final long TIMEOUT_MS = 5 * 60 * 1000;

    @Override
    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            try {
                boolean isNewConversation = request.getConversationId() == null;
                Long conversationId = getOrCreateConversation(request);

                boolean hasImage = request.getImageData() != null && !request.getImageData().isEmpty();
                String messageText = request.getMessage() != null ? request.getMessage() : "";

                // Save user message with image metadata if present
                String userMetadata = null;
                if (hasImage) {
                    ObjectNode metaNode = objectMapper.createObjectNode();
                    metaNode.put("imageData", request.getImageData());
                    metaNode.put("imageName", request.getImageName());
                    userMetadata = objectMapper.writeValueAsString(metaNode);
                }
                memoryService.addMessage(conversationId, MessageRole.USER, messageText, userMetadata);

                List<ChatMessage> history = new ArrayList<>(memoryService.getRecentMessages(conversationId, 20));

                // Replace the last UserMessage with multimodal version if image is present
                if (hasImage && !history.isEmpty()) {
                    ChatMessage lastMsg = history.get(history.size() - 1);
                    if (lastMsg instanceof UserMessage) {
                        String dataUri = buildDataUri(request.getImageData(), request.getImageName());
                        UserMessage multimodalMsg = UserMessage.from(
                                TextContent.from(messageText),
                                ImageContent.from(dataUri)
                        );
                        history.set(history.size() - 1, multimodalMsg);
                    }
                }

                // RAG retrieval with status events
                final List<RetrievalResult> retrievals;
                if (request.getKnowledgeBaseId() != null && !messageText.isBlank()) {
                    sendStatus(emitter, "正在查询知识库...");
                    retrievals = retrievalService.retrieve(
                            messageText,
                            request.getKnowledgeBaseId(),
                            ragProperties.getTopK()
                    );
                    sendStatus(emitter, "查询完成，找到 " + retrievals.size() + " 条相关文档");
                } else {
                    retrievals = Collections.emptyList();
                }

                String systemPrompt = buildRagSystemPrompt(retrievals);

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from(systemPrompt));
                messages.addAll(history);

                // Use multimodal model when image is present
                StreamingChatLanguageModel model = hasImage
                        ? modelHolder.getStreamingMultimodalModel()
                        : modelHolder.getStreamingChatModel();

                sendStatus(emitter, "正在生成回答...");

                StringBuilder responseBuilder = new StringBuilder();
                model.chat(messages, new StreamingChatResponseHandler() {

                    @Override
                    public void onPartialResponse(String token) {
                        try {
                            StreamingEvent event = StreamingEvent.builder()
                                    .type(StreamingEventType.TOKEN)
                                    .content(token)
                                    .build();
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(objectMapper.writeValueAsString(event)));
                            responseBuilder.append(token);
                        } catch (Exception e) {
                            log.error("Failed to send token", e);
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        try {
                            // Send citations (document name + similarity only)
                            for (RetrievalResult r : retrievals) {
                                CitationVO citation = CitationVO.builder()
                                        .documentId(Long.parseLong(r.documentId()))
                                        .documentName(r.documentName())
                                        .similarityScore((double) r.score())
                                        .chunkIndex(r.chunkIndex().intValue())
                                        .build();
                                StreamingEvent event = StreamingEvent.builder()
                                        .type(StreamingEventType.CITATION)
                                        .citation(citation)
                                        .build();
                                emitter.send(SseEmitter.event()
                                        .name("citation")
                                        .data(objectMapper.writeValueAsString(event)));
                            }

                            // Generate title for new conversations
                            if (isNewConversation) {
                                generateAndSendTitle(emitter, conversationId, messageText, responseBuilder.toString());
                            }

                            // Send done event
                            StreamingEvent doneEvent = StreamingEvent.builder()
                                    .type(StreamingEventType.DONE)
                                    .build();
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(objectMapper.writeValueAsString(doneEvent)));
                            emitter.complete();

                            // Save assistant message with metadata
                            String metadata = retrievals.isEmpty() ? null :
                                    objectMapper.writeValueAsString(
                                            retrievals.stream()
                                                    .map(r -> CitationVO.builder()
                                                            .documentId(Long.parseLong(r.documentId()))
                                                            .documentName(r.documentName())
                                                            .similarityScore((double) r.score())
                                                            .build())
                                                    .toList()
                                    );
                            memoryService.addMessage(conversationId, MessageRole.ASSISTANT,
                                    responseBuilder.toString(), metadata);

                        } catch (Exception e) {
                            log.error("Failed to complete stream", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            StreamingEvent errorEvent = StreamingEvent.builder()
                                    .type(StreamingEventType.ERROR)
                                    .content(error.getMessage())
                                    .build();
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(objectMapper.writeValueAsString(errorEvent)));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }
                });

            } catch (Exception e) {
                log.error("Chat stream failed", e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> log.warn("SSE error: {}", t.getMessage()));

        return emitter;
    }

    private Long getOrCreateConversation(ChatRequest request) {
        if (request.getConversationId() != null) {
            return request.getConversationId();
        }

        ConversationEntity conversation = new ConversationEntity();
        String title = (request.getMessage() != null && !request.getMessage().isBlank())
                ? request.getMessage().substring(0, Math.min(50, request.getMessage().length()))
                : "图片对话";
        conversation.setTitle(title);
        conversation.setKnowledgeBaseId(request.getKnowledgeBaseId());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);
        return conversation.getId();
    }

    private String buildRagSystemPrompt(List<RetrievalResult> retrievals) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 TOMO 助手，一个专业的知识库问答系统。请根据以下参考文档回答用户的问题。\n");
        prompt.append("如果参考文档中没有相关信息，请如实告知。\n\n");

        if (!retrievals.isEmpty()) {
            prompt.append("## 参考文档:\n");
            for (int i = 0; i < retrievals.size(); i++) {
                RetrievalResult r = retrievals.get(i);
                prompt.append(String.format("[%d] (相似度: %.2f)\n%s\n\n",
                        i + 1, r.score(), r.content()));
            }
            prompt.append("## 回答要求:\n");
            prompt.append("- 使用 [1], [2] 等编号引用来源\n");
            prompt.append("- 回答要准确、客观\n");
            prompt.append("- 如果不确定，请说明\n");
        }

        return prompt.toString();
    }

    private String buildDataUri(String base64Data, String fileName) {
        String mimeType = "image/png";
        if (fileName != null) {
            String ext = fileName.contains(".") ?
                    fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
            mimeType = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "bmp" -> "image/bmp";
                case "webp" -> "image/webp";
                default -> "image/png";
            };
        }
        return "data:" + mimeType + ";base64," + base64Data;
    }

    private void sendStatus(SseEmitter emitter, String statusText) {
        try {
            StreamingEvent event = StreamingEvent.builder()
                    .type(StreamingEventType.STATUS)
                    .content(statusText)
                    .build();
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to send status", e);
        }
    }

    private void generateAndSendTitle(SseEmitter emitter, Long conversationId, String userMessage, String assistantResponse) {
        try {
            String prompt = "请用一句话概括以下对话的主题，不超过20个字，不要加标点符号前缀：\n" +
                    "用户：" + userMessage + "\n" +
                    "助手：" + assistantResponse.substring(0, Math.min(500, assistantResponse.length()));
            String rawTitle = modelHolder.getChatModel().chat(prompt);
            String title = rawTitle.trim();
            if (title.length() > 50) {
                title = title.substring(0, 50);
            }

            // Update conversation title in DB
            final String finalTitle = title;
            conversationRepository.findById(conversationId).ifPresent(conv -> {
                conv.setTitle(finalTitle);
                conversationRepository.save(conv);
            });

            // Send title event to frontend
            StreamingEvent titleEvent = StreamingEvent.builder()
                    .type(StreamingEventType.TITLE)
                    .title(finalTitle)
                    .conversationId(conversationId)
                    .build();
            emitter.send(SseEmitter.event()
                    .name("title")
                    .data(objectMapper.writeValueAsString(titleEvent)));
        } catch (Exception e) {
            log.error("Failed to generate title", e);
        }
    }
}
