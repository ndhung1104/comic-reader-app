package com.group09.ComicReader.translationjob.client;

import com.group09.ComicReader.config.properties.TranslationWorkerProperties;
import com.group09.ComicReader.translationjob.client.dto.WorkerJobStatusResponse;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobRequest;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

@Component
public class TranslationWorkerClient {

    private final RestTemplate restTemplate;
    private final TranslationWorkerProperties workerProperties;

    public TranslationWorkerClient(RestTemplate restTemplate, TranslationWorkerProperties workerProperties) {
        this.restTemplate = restTemplate;
        this.workerProperties = workerProperties;
    }

    public WorkerSubmitJobResponse submitJob(WorkerSubmitJobRequest request) {
        String url = buildBaseUrl() + "/jobs";
        Instant start = Instant.now();
        ResponseEntity<WorkerSubmitJobResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request),
                WorkerSubmitJobResponse.class
        );
        ensureWithinTimeout(start);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RestClientException("Translation worker submit job failed");
        }
        return response.getBody();
    }

    public WorkerJobStatusResponse fetchJobStatus(String externalJobId) {
        String url = buildBaseUrl() + "/jobs/" + externalJobId;
        Instant start = Instant.now();
        ResponseEntity<WorkerJobStatusResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                WorkerJobStatusResponse.class
        );
        ensureWithinTimeout(start);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RestClientException("Translation worker fetch job status failed");
        }
        return response.getBody();
    }

    public WorkerJobStatusResponse fetchArtifacts(String externalJobId) {
        String url = buildBaseUrl() + "/jobs/" + externalJobId + "/artifacts";
        Instant start = Instant.now();
        ResponseEntity<WorkerJobStatusResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                WorkerJobStatusResponse.class
        );
        ensureWithinTimeout(start);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RestClientException("Translation worker fetch artifacts failed");
        }
        return response.getBody();
    }

    private String buildBaseUrl() {
        String baseUrl = workerProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("translation-worker.base-url is not configured");
        }
        String sanitized = baseUrl.trim();
        if (sanitized.endsWith("/")) {
            return sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized;
    }

    private void ensureWithinTimeout(Instant start) {
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        if (elapsedMs > workerProperties.getTimeoutMs()) {
            throw new RestClientException("Translation worker request timed out");
        }
    }
}
