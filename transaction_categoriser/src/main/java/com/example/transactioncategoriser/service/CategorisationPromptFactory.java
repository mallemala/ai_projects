package com.example.transactioncategoriser.service;

import com.example.transactioncategoriser.model.TransactionRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorisationPromptFactory {

    public String buildCategorisationPrompt(List<TransactionRecord> transactions) {
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
}
