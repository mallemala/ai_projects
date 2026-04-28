package com.example.transactioncategoriser.controller;

import com.example.transactioncategoriser.model.AnalysisResult;
import com.example.transactioncategoriser.model.CategorisedTransaction;
import com.example.transactioncategoriser.model.UploadViewModel;
import com.example.transactioncategoriser.service.GeminiCategorisationService;
import com.example.transactioncategoriser.service.PdfStatementParser;
import com.example.transactioncategoriser.service.TransactionAnalysisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class TransactionController {

    private final PdfStatementParser pdfStatementParser;
    private final GeminiCategorisationService geminiCategorisationService;
    private final TransactionAnalysisService transactionAnalysisService;

    public TransactionController(
            PdfStatementParser pdfStatementParser,
            GeminiCategorisationService geminiCategorisationService,
            TransactionAnalysisService transactionAnalysisService
    ) {
        this.pdfStatementParser = pdfStatementParser;
        this.geminiCategorisationService = geminiCategorisationService;
        this.transactionAnalysisService = transactionAnalysisService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("viewModel", UploadViewModel.empty());
        return "index";
    }

    @PostMapping("/analyse")
    public String analyse(
            MultipartFile file,
            Model model
    ) {
        try {
            List<CategorisedTransaction> categorisedTransactions = geminiCategorisationService.categorise(
                    pdfStatementParser.parse(file)
            );
            AnalysisResult result = transactionAnalysisService.analyse(categorisedTransactions);
            model.addAttribute("viewModel", new UploadViewModel(result, null));
            return "index";
        } catch (RuntimeException ex) {
            model.addAttribute("viewModel", new UploadViewModel(null, ex.getMessage()));
            return "index";
        }
    }
}
