package com.example.transactioncategoriser.service;

import com.example.transactioncategoriser.model.AnalysisResult;
import com.example.transactioncategoriser.model.CategorisedTransaction;
import com.example.transactioncategoriser.model.TransactionRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TransactionProcessingService {

    private final PdfStatementParser pdfStatementParser;
    private final GeminiCategorisationService geminiCategorisationService;
    private final TransactionAnalysisService transactionAnalysisService;

    public TransactionProcessingService(
            PdfStatementParser pdfStatementParser,
            GeminiCategorisationService geminiCategorisationService,
            TransactionAnalysisService transactionAnalysisService
    ) {
        this.pdfStatementParser = pdfStatementParser;
        this.geminiCategorisationService = geminiCategorisationService;
        this.transactionAnalysisService = transactionAnalysisService;
    }

    public AnalysisResult analyse(MultipartFile file) {
        List<TransactionRecord> extractedTransactions = pdfStatementParser.parse(file);
        List<CategorisedTransaction> categorisedTransactions = geminiCategorisationService.categorise(extractedTransactions);
        return transactionAnalysisService.analyse(categorisedTransactions);
    }
}
