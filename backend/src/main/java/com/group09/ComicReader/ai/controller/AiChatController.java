package com.group09.ComicReader.ai.controller;

import com.group09.ComicReader.ai.dto.AiChatRequest;
import com.group09.ComicReader.ai.dto.AiChatResponse;
import com.group09.ComicReader.ai.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request.getMessage(), request.getContext()));
    }
}

