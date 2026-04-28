package com.example.transactioncategoriser.service;

import com.example.transactioncategoriser.config.GeminiProperties;
import com.example.transactioncategoriser.model.TransactionRecord;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfStatementParser {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\[(.*)]", Pattern.DOTALL);

    private final WebClient webClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public PdfStatementParser(WebClient webClient, GeminiProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<TransactionRecord> parse(MultipartFile file) {
        validateFile(file);
        ensureApiKeyPresent();

        String statementText = extractText(file);
        if (!StringUtils.hasText(statementText)) {
            throw new IllegalArgumentException("The uploaded PDF did not contain readable statement text.");
        }

        GeminiResponse response = webClient.post()
                .uri(properties.endpoint() + "/" + properties.model() + ":generateContent?key=" + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GeminiRequest(List.of(new Content(List.of(new Part(buildPrompt(statementText)))))))
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block();

        String rawText = extractResponseText(response);
        List<ExtractedTransaction> extractedTransactions = parseRows(rawText);
        if (extractedTransactions.isEmpty()) {
            throw new IllegalStateException("No transactions could be extracted from the uploaded PDF statement.");
        }

        return extractedTransactions.stream()
                .map(row -> new TransactionRecord(
                        LocalDate.parse(row.date()),
                        row.description().trim(),
                        row.amount()
                ))
                .toList();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a PDF bank statement.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF bank statements are supported.");
        }
    }

    private void ensureApiKeyPresent() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Gemini API key is missing. Set GEMINI_API_KEY before running the app.");
        }
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read the uploaded PDF statement.", ex);
        }
    }

    private String buildPrompt(String statementText) {
        return """
                Extract the bank transactions from this PDF bank statement text.
                Return JSON only.

                Output format:
                [
                  {"date":"2026-04-01","description":"Tesco Superstore","amount":-42.65},
                  {"date":"2026-04-02","description":"Salary Employer Ltd","amount":2500.00}
                ]

                Rules:
                - Include only real transactions from the statement.
                - Ignore balances, opening balance, closing balance, page headers, page footers, summaries, and non-transaction text.
                - Convert dates to ISO format yyyy-MM-dd.
                - Use negative amounts for spending/debits and positive amounts for credits/income/refunds.
                - Preserve statement order.
                - Do not include markdown or explanations.

                Statement text:
                """ + statementText;
    }

    private String extractResponseText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini did not return any candidates while parsing the PDF.");
        }

        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini response did not contain parsed transaction text.");
        }

        return candidate.content().parts().stream()
                .map(PartResponse::text)
                .filter(StringUtils::hasText)
                .reduce("", String::concat);
    }

    private List<ExtractedTransaction> parseRows(String rawText) {
        String json = rawText.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(json);
        if (matcher.find()) {
            json = "[" + matcher.group(1) + "]";
        }

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ExtractedTransaction.class)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse extracted PDF transactions from Gemini response: " + rawText, ex);
        }
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
    private record ExtractedTransaction(
            @JsonProperty("date") String date,
            @JsonProperty("description") String description,
            @JsonProperty("amount") BigDecimal amount
    ) {
    }
}
