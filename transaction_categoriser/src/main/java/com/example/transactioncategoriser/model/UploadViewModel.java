package com.example.transactioncategoriser.model;

public record UploadViewModel(
        AnalysisResult analysisResult,
        String errorMessage
) {
    public static UploadViewModel empty() {
        return new UploadViewModel(null, null);
    }
}
