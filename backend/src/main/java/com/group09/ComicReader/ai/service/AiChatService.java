package com.group09.ComicReader.ai.service;

import com.group09.ComicReader.ai.dto.AiChatResponse;
import com.group09.ComicReader.ai.entity.AiFeature;
import com.group09.ComicReader.ai.service.client.AiClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AiChatService {

    private final AiClient aiClient;
    private final AiUsageService aiUsageService;

    public AiChatService(AiClient aiClient, AiUsageService aiUsageService) {
        this.aiClient = aiClient;
        this.aiUsageService = aiUsageService;
    }

    @Transactional
    public AiChatResponse chat(String message, String context) {
        if (message == null || message.isBlank()) {
            throw new com.group09.ComicReader.common.exception.BadRequestException("Message is required");
        }

        AiUsageService.UsageContext usageContext = aiUsageService.beginUsage(
                AiFeature.AI_CHAT,
                "chat-" + System.currentTimeMillis(),
                message.length(),
                "chat"
        );

        try {
            String systemPrompt = buildSystemPrompt(context);
            String reply = aiClient.generate(systemPrompt, message);

            aiUsageService.completeSuccess(usageContext, aiClient.getProviderKey(), aiClient.getModel(), reply.length(), "success");

            return new AiChatResponse(reply, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        } catch (Exception e) {
            aiUsageService.completeFailure(usageContext, aiClient.getProviderKey(), aiClient.getModel(), e.getMessage());
            throw e;
        }
    }

    private String buildSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly and knowledgeable comic book assistant for a manga/manhwa reading app. ");
        sb.append("You help users discover comics, answer questions about genres, plots, characters, and reading recommendations. ");
        sb.append("Keep responses concise, engaging, and under 200 words when possible. ");
        sb.append("If the user asks about specific comic content, provide helpful summaries or guidance. ");
        sb.append("If you don't know something, be honest and suggest alternatives.");
        if (context != null && !context.isBlank()) {
            sb.append("\n\nAdditional context: ").append(context);
        }
        return sb.toString();
    }
}

