package com.group09.ComicReader.translationjob.client;

import com.group09.ComicReader.config.properties.TtsWorkerProperties;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchRequest;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

@Component
public class TtsWorkerClient {

    private final RestTemplate restTemplate;
    private final TtsWorkerProperties properties;

    public TtsWorkerClient(RestTemplate restTemplate, TtsWorkerProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public TtsWorkerSynthesizeBatchResponse synthesizeBatch(TtsWorkerSynthesizeBatchRequest request) {
        String url = buildBaseUrl() + "/tts/synthesize-batch";
        Instant start = Instant.now();
        ResponseEntity<TtsWorkerSynthesizeBatchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request),
                TtsWorkerSynthesizeBatchResponse.class
        );
        ensureWithinTimeout(start);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RestClientException("TTS worker synthesize batch failed");
        }
        return response.getBody();
    }

    private String buildBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("tts-worker.base-url is not configured");
        }
        String sanitized = baseUrl.trim();
        if (sanitized.endsWith("/")) {
            return sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized;
    }

    private void ensureWithinTimeout(Instant start) {
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        if (elapsedMs > properties.getTimeoutMs()) {
            throw new RestClientException("TTS worker request timed out");
        }
    }
}
