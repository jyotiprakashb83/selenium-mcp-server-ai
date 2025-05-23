package org.example;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


public class LlmClient {
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final CloseableHttpClient httpClient;

    public LlmClient(String endpoint, String model, String apiKey) {
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
        this.httpClient = HttpClients.createDefault();
    }

    public String queryLlm(String prompt) throws Exception {
        String requestBody;
        if (endpoint.contains("localhost:11434")) {
            // Ollama API format
            requestBody = new ObjectMapper().writeValueAsString(
                    Map.of("model", model, "prompt", prompt, "stream", false)
            );
        } else {
            // OpenAI-compatible API format
            requestBody = new ObjectMapper().writeValueAsString(
                    Map.of("model", model, "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ))
            );
        }

        HttpPost post = new HttpPost(endpoint + (endpoint.contains("localhost:11434") ? "/api/generate" : "/v1/chat/completions"));
        post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        post.setHeader("Content-Type", "application/json");
        if (!apiKey.isEmpty()) {
            post.setHeader("Authorization", "Bearer " + apiKey);
        }

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            if (endpoint.contains("localhost:11434")) {
                return mapper.readTree(responseBody).get("response").asText();
            } else {
                return mapper.readTree(responseBody).get("choices").get(0).get("message").get("content").asText();
            }
        }
    }
}
