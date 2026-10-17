package com.territorypaint.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API Client for authentication, profile, leaderboard, and match history.
 * Requirements: 1.7, 1.11, 10.1, 10.6, 11.1
 */
public class RestClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public RestClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public CompletableFuture<Map<String, Object>> register(String username, String password) {
        Map<String, String> body = Map.of("username", username, "password", password, "fullName", username);
        return sendPost("/api/auth/register", body, false);
    }

    public CompletableFuture<Map<String, Object>> login(String username, String password) {
        Map<String, String> body = Map.of("username", username, "password", password);
        return sendPost("/api/auth/login", body, false).thenApply(res -> {
            if (res.containsKey("token")) {
                this.authToken = (String) res.get("token");
            }
            return res;
        });
    }

    public CompletableFuture<Map<String, Object>> getProfile() {
        return sendGet("/api/users/profile", true);
    }

    public CompletableFuture<Map<String, Object>> getLeaderboard(int page, int size) {
        return sendGet("/api/users/leaderboard?page=" + page + "&size=" + size, true);
    }

    public CompletableFuture<Map<String, Object>> getMatchHistory(int page, int size) {
        Long myId = com.territorypaint.client.state.ClientState.getInstance().getUserId();
        return sendGet("/api/matches/history?page=" + page + "&size=" + size, true)
                .thenCompose(res -> {
                    if (res != null && res.containsKey("error") && myId != null) {
                        return sendGet("/api/users/" + myId + "/matches?page=" + page + "&size=" + size, true);
                    }
                    return CompletableFuture.completedFuture(res);
                });
    }

    private CompletableFuture<Map<String, Object>> sendPost(String endpoint, Object bodyObj, boolean requireAuth) {
        try {
            String jsonBody = objectMapper.writeValueAsString(bodyObj);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (requireAuth && authToken != null) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::parseResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Map<String, Object>> sendGet(String endpoint, boolean requireAuth) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("Accept", "application/json")
                    .GET();

            if (requireAuth && authToken != null) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::parseResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(HttpResponse<String> response) {
        try {
            if (response.body() == null || response.body().isBlank()) {
                return Map.of("error", "HTTP_" + response.statusCode(), "message", "HTTP error " + response.statusCode());
            }
            String body = response.body().trim();
            if (body.startsWith("[")) {
                java.util.List<?> list = objectMapper.readValue(body, java.util.List.class);
                return Map.of("items", list, "content", list, "totalElements", list.size(), "totalItems", list.size(), "totalPages", 1);
            }
            Map<String, Object> rawMap = objectMapper.readValue(body, Map.class);
            Map<String, Object> map = new java.util.HashMap<>(rawMap);
            if (map.containsKey("items") && !map.containsKey("content")) {
                map.put("content", map.get("items"));
            } else if (map.containsKey("content") && !map.containsKey("items")) {
                map.put("items", map.get("content"));
            }
            if (response.statusCode() >= 400) {
                // Ensure error or message exists if backend response didn't provide any key
                if (!map.containsKey("message") && !map.containsKey("error") && !map.containsKey("fields")) {
                    return Map.of("error", "HTTP_" + response.statusCode(), "message", "Server error " + response.statusCode());
                }
            }
            return map;
        } catch (Exception e) {
            String rawBody = response.body() != null ? response.body().trim() : "";
            String msg = rawBody.isEmpty() ? e.getMessage() : rawBody;
            return Map.of("error", "HTTP_" + response.statusCode(), "message", msg);
        }
    }
}
