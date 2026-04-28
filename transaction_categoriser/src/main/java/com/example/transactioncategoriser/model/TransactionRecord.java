package com.example.transactioncategoriser.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRecord(
        LocalDate date,
        String description,
        BigDecimal amount
) {
}
