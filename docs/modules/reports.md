# Reports Module

**Controller:** `ReportsController`  
**Service:** `ReportsService`  
**Generator:** `ReportFileGenerator`  
**Frontend:** `features/reports/`

## Report Types

`PROFIT_AND_LOSS`, `CASH_FLOW`, `REVENUE_SUMMARY`, `EXPENSE_SUMMARY`, `TAX_SUMMARY`, `FOP_SUMMARY`

## Formats

PDF (`OpenPdfReportRenderer`), Excel (`PoiReportRenderer`), CSV

## Job Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING: POST /generate
    PENDING --> GENERATING: async process
    GENERATING --> COMPLETED: file in BYTEA
    GENERATING --> FAILED: error
    COMPLETED --> [*]: GET /download
```

## Storage

`report_jobs.file_content` BYTEA — consider S3 for production scale.

## Integration

On completion: `NotificationGeneratorService.notifyReportCompleted()` + optional task for review.
