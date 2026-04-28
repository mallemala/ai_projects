package com.example.transactioncategoriser.service;

import com.example.transactioncategoriser.config.GeminiProperties;
import com.example.transactioncategoriser.model.CategorisedTransaction;
import com.example.transactioncategoriser.model.TransactionRecord;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiCategorisationService {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\[(.*)]", Pattern.DOTALL);

    private final WebClient webClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiCategorisationService(WebClient webClient, GeminiProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<CategorisedTransaction> categorise(List<TransactionRecord> transactions) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Gemini API key is missing. Set GEMINI_API_KEY before running the app.");
        }

        String payload = buildPrompt(transactions);
        GeminiResponse response = webClient.post()
                .uri(properties.endpoint() + "/" + properties.model() + ":generateContent?key=" + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GeminiRequest(List.of(new Content(List.of(new Part(payload))))))
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block();

        String rawText = extractText(response);
        List<ClassificationRow> rows = parseRows(rawText);

        if (rows.size() != transactions.size()) {
            throw new IllegalStateException("Gemini returned " + rows.size() + " classifications for " + transactions.size() + " transactions.");
        }

        List<CategorisedTransaction> results = new ArrayList<>();
        for (int i = 0; i < transactions.size(); i++) {
            TransactionRecord transaction = transactions.get(i);
            ClassificationRow row = rows.get(i);
            results.add(new CategorisedTransaction(
                    transaction.date(),
                    transaction.description(),
                    transaction.amount(),
                    sanitiseCategory(row.category()),
                    sanitiseMerchant(row.merchant(), transaction.description())
            ));
        }

        return results;
    }

    private String buildPrompt(List<TransactionRecord> transactions) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                Categorise the following bank transactions.
                Return JSON only.
                Output format:
                [
                  {"category":"Groceries","merchant":"Tesco"},
                  {"category":"Income","merchant":"Employer Ltd"}
                ]

                Rules:
                - Keep the same order as the input transactions.
                - Categories should be short household finance labels such as Groceries, Income, Insurance, Credit Card, Dining, Travel, Utilities, Rent, Shopping, Healthcare, Entertainment, Savings, Cash Withdrawal, Transfer, Subscriptions, Education, Taxes, or Other.
                - Merchant should be the best store, company, employer, or payee inferred from the description.
                - Do not include explanations, markdown, or extra text.

                Transactions:
                """);

        for (int i = 0; i < transactions.size(); i++) {
            TransactionRecord transaction = transactions.get(i);
            builder.append(i + 1)
                    .append(". date=")
                    .append(transaction.date())
                    .append(", amount=")
                    .append(transaction.amount())
                    .append(", description=")
                    .append(transaction.description())
                    .append('\n');
        }
        return builder.toString();
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini did not return any candidates.");
        }

        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini response did not contain text content.");
        }

        return candidate.content().parts().stream()
                .map(PartResponse::text)
                .filter(StringUtils::hasText)
                .reduce("", String::concat);
    }

    private List<ClassificationRow> parseRows(String rawText) {
        String json = rawText.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(json);
        if (matcher.find()) {
            json = "[" + matcher.group(1) + "]";
        }

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ClassificationRow.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse Gemini response as JSON: " + rawText, ex);
        }
    }

    private String sanitiseCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "Other";
        }
        String cleaned = category.trim();
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
    }

    private String sanitiseMerchant(String merchant, String fallback) {
        if (!StringUtils.hasText(merchant)) {
            return fallback;
        }
        return merchant.trim();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClassificationRow(
            @JsonProperty("category") String category,
            @JsonProperty("merchant") String merchant
    ) {
    }
}
