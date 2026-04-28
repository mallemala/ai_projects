package com.example.transactioncategoriser.model;

import java.math.BigDecimal;

public record MerchantSummary(
        String merchant,
        BigDecimal totalAmount,
        int transactionCount
) {
}
