package com.example.transactioncategoriser.service;

import com.example.transactioncategoriser.model.AnalysisResult;
import com.example.transactioncategoriser.model.CategorisedTransaction;
import com.example.transactioncategoriser.model.CategorySummary;
import com.example.transactioncategoriser.model.MerchantSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionAnalysisService {

    public AnalysisResult analyse(List<CategorisedTransaction> transactions) {
        List<CategorySummary> categorySummaries = transactions.stream()
                .collect(Collectors.groupingBy(CategorisedTransaction::category))
                .entrySet()
                .stream()
                .map(entry -> new CategorySummary(
                        entry.getKey(),
                        totalTransactions(entry.getValue()),
                        entry.getValue().size(),
                        merchantSummaries(entry.getValue())
                ))
                .sorted(Comparator.comparing((CategorySummary summary) -> summary.totalAmount().abs()).reversed())
                .toList();

        BigDecimal grandTotal = totalTransactions(transactions);
        BigDecimal totalIncome = transactions.stream()
                .map(CategorisedTransaction::amount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpending = transactions.stream()
                .map(CategorisedTransaction::amount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
        return new AnalysisResult(transactions, categorySummaries, grandTotal, totalIncome, totalSpending);
    }

    private List<MerchantSummary> merchantSummaries(List<CategorisedTransaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(CategorisedTransaction::merchant))
                .entrySet()
                .stream()
                .map(entry -> new MerchantSummary(
                        entry.getKey(),
                        totalTransactions(entry.getValue()),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing((MerchantSummary summary) -> summary.totalAmount().abs()).reversed())
                .toList();
    }

    private BigDecimal totalTransactions(List<CategorisedTransaction> rows) {
        return rows.stream()
                .map(CategorisedTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
