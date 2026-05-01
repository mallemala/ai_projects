package com.example.transactioncategoriser.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsonArrayParser {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\[(.*)]", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public JsonArrayParser() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> List<T> parseList(String rawText, Class<T> elementType, String errorMessage) {
        String json = extractJsonArray(rawText);

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(errorMessage + ": " + rawText, ex);
        }
    }

    private String extractJsonArray(String rawText) {
        String json = rawText.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(json);
        if (matcher.find()) {
            return "[" + matcher.group(1) + "]";
        }
        return json;
    }
}
