package com.flowiq.audit.support;

import com.flowiq.audit.AuditEventType;
import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.dto.response.ReportJobResponse;
import com.flowiq.dto.response.TransactionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

public final class AuditMetadataBuilder {

    private AuditMetadataBuilder() {
    }

    public static Map<String, Object> build(AuditEventType eventType, Object[] args, Object result, Exception error) {
        Map<String, Object> metadata = new HashMap<>();

        switch (eventType) {
            case TRANSACTION_CREATE, TRANSACTION_UPDATE -> enrichTransaction(metadata, result);
            case TRANSACTION_DELETE -> { /* resource id captured separately */ }
            case IMPORT_UPLOAD -> enrichImport(metadata, args, result);
            case REPORT_GENERATE -> enrichReport(metadata, result);
            case AI_ACCOUNTANT_CHAT -> enrichAiChat(metadata, args, result);
            default -> { }
        }

        if (error != null) {
            metadata.put("errorType", error.getClass().getSimpleName());
            if (error.getMessage() != null) {
                metadata.put("errorMessage", error.getMessage());
            }
        }

        return metadata;
    }

    private static void enrichTransaction(Map<String, Object> metadata, Object result) {
        Object body = unwrap(result);
        if (body instanceof TransactionResponse transaction) {
            metadata.put("entityId", transaction.getId());
            metadata.put("type", transaction.getType());
            metadata.put("amount", transaction.getAmount());
            metadata.put("category", transaction.getCategory());
            if (transaction.getTransactionDate() != null) {
                metadata.put("transactionDate", transaction.getTransactionDate().toString());
            }
        }
    }

    private static void enrichImport(Map<String, Object> metadata, Object[] args, Object result) {
        for (Object arg : args) {
            if (arg instanceof MultipartFile file) {
                metadata.put("fileName", file.getOriginalFilename());
                metadata.put("fileSize", file.getSize());
            }
        }
        Object body = unwrap(result);
        if (body instanceof ImportJobResponse job) {
            metadata.put("jobId", job.getId());
            metadata.put("fileName", job.getFileName());
            metadata.put("fileSize", job.getFileSize());
            metadata.put("bankFormat", job.getBankFormat());
            metadata.put("rowsImported", job.getRowsImported());
            metadata.put("errorsCount", job.getErrorsCount());
        }
    }

    private static void enrichReport(Map<String, Object> metadata, Object result) {
        Object body = unwrap(result);
        if (body instanceof ReportJobResponse job) {
            metadata.put("jobId", job.getId());
            metadata.put("reportType", job.getReportType());
            metadata.put("format", job.getFormat());
            metadata.put("fileSize", job.getFileSize());
            if (job.getPeriodFrom() != null) {
                metadata.put("periodFrom", job.getPeriodFrom().toString());
            }
            if (job.getPeriodTo() != null) {
                metadata.put("periodTo", job.getPeriodTo().toString());
            }
        }
    }

    private static void enrichAiChat(Map<String, Object> metadata, Object[] args, Object result) {
        for (Object arg : args) {
            if (arg instanceof AIAccountantChatRequest request && request.getMessage() != null) {
                metadata.put("messageLength", request.getMessage().length());
                metadata.put("messageHash", AuditMetadataSanitizer.sha256(request.getMessage()));
            }
        }
        Object body = unwrap(result);
        if (body instanceof AIAccountantChatResponse response && response.getReply() != null) {
            metadata.put("replyLength", response.getReply().length());
        }
    }

    private static Object unwrap(Object result) {
        if (result instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
            return responseEntity.getBody();
        }
        return result;
    }
}
