package com.example.transactioncategoriser.service;

import com.example.transactioncategoriser.config.GeminiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class GeminiClient {

    private final WebClient webClient;
    private final GeminiProperties properties;

    public GeminiClient(WebClient webClient, GeminiProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public String generateText(String prompt, String responseErrorMessage) {
        ensureApiKeyPresent();

        GeminiResponse response = webClient.post()
                .uri(properties.endpoint() + "/" + properties.model() + ":generateContent?key=" + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GeminiRequest(List.of(new Content(List.of(new Part(prompt))))))
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block();

        return extractText(response, responseErrorMessage);
    }

    private void ensureApiKeyPresent() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Gemini API key is missing. Set GEMINI_API_KEY before running the app.");
        }
    }

    private String extractText(GeminiResponse response, String responseErrorMessage) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException(responseErrorMessage);
        }

        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException(responseErrorMessage);
        }

        String text = candidate.content().parts().stream()
                .map(PartResponse::text)
                .filter(StringUtils::hasText)
                .reduce("", String::concat);

        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(responseErrorMessage);
        }

        return text;
    }

    private record GeminiRequest(List<Content> contents) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(ContentResponse content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentResponse(List<PartResponse> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PartResponse(String text) {
    }
}
