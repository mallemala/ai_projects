# Transaction Categoriser

Spring Boot application that accepts a PDF bank statement, asks Gemini to extract and categorise the transactions, and shows:

- category totals such as `Groceries`, `Income`, `Insurance`, `Credit Card`
- merchant or store-level totals inside each category
- a full categorised transaction table in the UI

## Features

- PDF bank statement upload through a web UI
- PDF text extraction with Gemini-based transaction extraction
- Gemini AI categorisation with merchant extraction
- category summary cards and store-level drill-down
- responsive dashboard built with Thymeleaf and custom CSS

## Tech Stack

- Java 21
- Spring Boot 3
- Thymeleaf
- Apache PDFBox
- Gemini API via `WebClient`

## Prerequisites

- Java 21 or newer
- Maven 3.9 or newer
- A Gemini API key

## Configuration

Set the Gemini API key before starting the application.

```bash
export GEMINI_API_KEY="your-api-key"
```

Optional settings:

```bash
export GEMINI_MODEL="gemini-2.5-flash"
export GEMINI_ENDPOINT="https://generativelanguage.googleapis.com/v1beta/models"
```

## Expected PDF Input

Use a text-based bank statement PDF. The app extracts the statement text and asks Gemini to return structured transactions with:

- `date`
- `description`
- `amount`

Scanned image-only PDFs may need OCR before upload if the PDF does not contain selectable text.

## Run The App

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

## Build

```bash
mvn clean test
```

## How It Works

1. Upload a PDF bank statement.
2. The backend extracts text from the PDF.
3. The app sends the statement text to Gemini and asks for structured transactions.
4. The UI renders category totals and merchant-level breakdowns.

## Notes

- Spending is expected as negative and income as positive after Gemini extracts the transaction data.
- Best results come from statement PDFs with clean text rather than scanned images.
- If Gemini returns invalid JSON or the API key is missing, the UI shows an error message.
