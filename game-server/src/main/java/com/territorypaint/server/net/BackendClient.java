package com.territorypaint.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.dto.StandingsDTO;
import com.territorypaint.common.payload.LoginResultPayload;
import com.territorypaint.common.payload.ResultSavedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for communicating with Backend_API REST endpoints.
 * Requirements: 2.3, 2.4, 9.3, 9.4, 19.4, 19.5
 */
public class BackendClient {

    private static final Logger logger = LoggerFactory.getLogger(BackendClient.class);

    private final String backendBaseUrl;
    private final String serviceKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BackendClient(String backendBaseUrl, String serviceKey) {
        this.backendBaseUrl = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1) : backendBaseUrl;
        this.serviceKey = serviceKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static class ValidationResult {
        public boolean valid;
        public Long userId;
        public String username;
        public Long totalScore;
        public Instant expiresAt;

        public ValidationResult(boolean valid, Long userId, String username, Long totalScore, Instant expiresAt) {
            this.valid = valid;
            this.userId = userId;
            this.username = username;
            this.totalScore = totalScore;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Validate JWT token with backend POST /api/internal/sessions/validate.
     * Requirements: 2.3, 2.4
     */
    public ValidationResult validateSession(String token) {
        try {
            Map<String, String> body = Map.of("token", token);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendBaseUrl + "/api/internal/sessions/validate"))
                    .header("Content-Type", "application/json")
                    .header("X-Service-Key", serviceKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<?, ?> map = objectMapper.readValue(response.body(), Map.class);
                Boolean valid = (Boolean) map.get("valid");
                if (Boolean.TRUE.equals(valid)) {
                    Long userId = ((Number) map.get("userId")).longValue();
                    String username = (String) map.get("username");
                    Long totalScore = ((Number) map.get("totalScore")).longValue();
                    String expStr = (String) map.get("expiresAt");
                    Instant expiresAt = expStr != null ? Instant.parse(expStr) : Instant.now().plusSeconds(7200);

                    return new ValidationResult(true, userId, username, totalScore, expiresAt);
                }
            } else {
                logger.warn("Session validation failed with HTTP status {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.error("Error connecting to backend for session validation: {}", e.getMessage());
        }
        return new ValidationResult(false, null, null, null, null);
    }

    /**
     * Submit match result to POST /api/matches/save.
     * Requirements: 9.3, 9.4, 9.5
     */
    public HttpResponse<String> saveMatch(String jsonPayload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(backendBaseUrl + "/api/matches/save"))
                .header("Content-Type", "application/json")
                .header("X-Service-Key", serviceKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(5))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
