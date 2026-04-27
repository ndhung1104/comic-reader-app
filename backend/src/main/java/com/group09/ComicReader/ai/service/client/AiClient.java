package com.group09.ComicReader.ai.service.client;

public interface AiClient {
    String getProviderKey();
    String getModel();
    String generate(String systemPrompt, String userPrompt);
}
