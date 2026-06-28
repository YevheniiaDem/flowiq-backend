# CSV Import Flow

**As-built:** 2026-06-28  
**Backend:** `ImportController`, `ImportService`, `importcsv.*`  
**Frontend:** `features/imports`

## Overview

Bank data enters FlowIQ exclusively via **CSV upload** (max 10 MB). No live bank API integration exists. Parsed rows become `transactions`; downstream modules read from that table.

## High-Level Flow

```mermaid
flowchart LR
    CSV[User CSV file] --> UP[POST /api/imports/upload]
    UP --> IS[ImportService]
    IS --> RESOLVE[CsvImportStrategyResolver]
    RESOLVE --> PARSE[Monobank / PrivatBank / Universal]
    PARSE --> CAT[CategorizationEngine]
    CAT --> DEDUP[Duplicate check]
    DEDUP --> SAVE[TransactionRepository.saveAll]
    IS --> JOB[import_jobs status]
    IS --> NOTIF[NotificationGeneratorService]
    IS --> TASK[TaskGeneratorService]
    SAVE --> TXN[(transactions)]
```

## Detailed Upload Sequence

```mermaid
sequenceDiagram
    participant UI as ImportsView
    participant IC as ImportController
    participant IS as ImportService
    participant IJR as ImportJobRepository
    participant RES as CsvImportStrategyResolver
    participant STR as CsvImportStrategy
    participant CE as CategorizationEngine
    participant TR as TransactionRepository
    participant NGS as NotificationGeneratorService
    participant TGS as TaskGeneratorService
    participant DB as PostgreSQL

    UI->>IC: POST /api/imports/upload (multipart CSV)
    IC->>IS: upload(file)
    IS->>IS: Validate .csv, max 10MB, non-empty
    IS->>IJR: save ImportJob PROCESSING
    IS->>NGS: notifyImportProcessing
    IS->>RES: resolve(csvContent)
    RES->>STR: select strategy by header/content
    STR->>STR: parse rows → ParsedTransactionRow[]
    loop each row
        IS->>TR: existsDuplicate(user, date, amount, type, desc)
        alt duplicate
            IS->>IS: skip row
        else new row
            IS->>CE: categorize(description, type, category)
            CE-->>IS: CategorizationResult
            IS->>IS: build Transaction entity
        end
    end
    IS->>TR: saveAll(transactions)
    IS->>IS: Set job status COMPLETED / PARTIAL / FAILED
    IS->>IJR: save final job
    alt imported > 0
        IS->>NGS: notifyImportCompleted
        IS->>TGS: createImportReviewTask
    else failed
        IS->>NGS: notifyImportFailed
    else partial
        IS->>NGS: notifyImportPartial
    end
    IS-->>UI: ImportJobResponse
```

## Parser Strategy Selection

```mermaid
flowchart TD
    A[CSV content] --> B{CsvImportStrategyResolver}
    B -->|Monobank headers| C[MonobankCsvStrategy]
    B -->|PrivatBank headers| D[PrivatBankCsvStrategy]
    B -->|Fallback| E[UniversalCsvStrategy]
    C --> F[ParsedTransactionRow list]
    D --> F
    E --> F
```

**Package:** `com.flowiq.importcsv`

## Categorization Pipeline

```mermaid
flowchart LR
    ROW[Parsed row] --> DCR[DefaultCategoryRules keywords]
    DCR --> CP{CategorizationProvider beans?}
    CP -->|None today| FALL[Fallback category]
    CP -->|Future LLM| EXT[External provider]
    DCR --> RESULT[CategorizationResult]
    RESULT --> TXN[Transaction.autoCategorized flag]
```

## Import Job States

| Status | Condition |
|--------|-----------|
| `PROCESSING` | Job created, parse in progress |
| `COMPLETED` | All rows imported, zero errors |
| `PARTIAL` | Some rows imported, some skipped/invalid |
| `FAILED` | Zero imported with errors, or parse failure |

## Downstream Consumers

Once `transactions` are populated:

```mermaid
flowchart TB
    TXN[(transactions)] --> DASH[Dashboard]
    TXN --> AN[Analytics]
    TXN --> FC[Forecasts]
    TXN --> AI[AI Accountant]
    TXN --> REP[Reports]
    TXN --> NRE[NotificationRuleEngine]
    TXN --> TRE[TaskRuleEngine]
```

## API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/imports/upload` | Upload CSV |
| GET | `/api/imports` | List jobs + stats |
| GET | `/api/imports/{id}` | Job detail |

## Related

- [integration-architecture.md](../integration-architecture.md)
- [Transactions Module](../../modules/transactions.md)
- [SRS §3.4](../product/SRS.md)
