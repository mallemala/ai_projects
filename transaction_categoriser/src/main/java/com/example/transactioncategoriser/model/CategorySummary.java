package com.example.transactioncategoriser.model;

import java.math.BigDecimal;
import java.util.List;

public record CategorySummary(
        String category,
        BigDecimal totalAmount,
        int transactionCount,
        List<MerchantSummary> merchants
) {
}
