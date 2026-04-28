package com.example.transactioncategoriser.model;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisResult(
        List<CategorisedTransaction> transactions,
        List<CategorySummary> categorySummaries,
        BigDecimal grandTotal,
        BigDecimal totalIncome,
        BigDecimal totalSpending
) {
}
