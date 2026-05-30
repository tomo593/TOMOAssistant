package com.tomo.tomoassistant.service.multimodal;

import com.tomo.tomoassistant.config.LlmModelHolder;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class ImageDescriptionServiceImpl implements ImageDescriptionService {

    private final LlmModelHolder modelHolder;

    public ImageDescriptionServiceImpl(LlmModelHolder modelHolder) {
        this.modelHolder = modelHolder;
    }

    @Override
    public String generateDescription(byte[] imageData, String fileName) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String mimeType = detectMimeType(fileName);
            String dataUri = "data:" + mimeType + ";base64," + base64Image;

            UserMessage userMessage = UserMessage.from(
                    TextContent.from("请详细描述这张图片的内容，包括图片中的文字、物体、场景、颜色等所有可见信息。描述应该足够详细，以便用于知识库检索。请用中文回答。"),
                    ImageContent.from(dataUri)
            );

            ChatResponse response = modelHolder.getMultimodalModel().chat(userMessage);
            String description = response.aiMessage().text();
            log.info("Generated image description for: {}, length: {}", fileName, description.length());
            return description;
        } catch (Exception e) {
            log.error("Failed to generate image description for: {}", fileName, e);
            return "图片: " + fileName;
        }
    }

    private String detectMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }
}
