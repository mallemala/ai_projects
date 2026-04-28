package com.example.transactioncategoriser.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CategorisedTransaction(
        LocalDate date,
        String description,
        BigDecimal amount,
        String category,
        String merchant
) {
}
