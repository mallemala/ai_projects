package com.example.transactioncategoriser.service;

import org.springframework.stereotype.Component;

@Component
public class StatementPromptFactory {

    public String buildExtractionPrompt(String statementText) {
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
}
