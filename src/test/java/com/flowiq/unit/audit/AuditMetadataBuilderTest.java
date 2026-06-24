package com.flowiq.unit.audit;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.support.AuditMetadataBuilder;
import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.dto.response.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditMetadataBuilder unit tests")
class AuditMetadataBuilderTest {

    @Test
    @DisplayName("builds transaction metadata without secrets")
    void buildsTransactionMetadata() {
        TransactionResponse transaction = TransactionResponse.builder()
                .id(10L)
                .amount(new BigDecimal("1500.00"))
                .category("Rent")
                .transactionDate(LocalDate.of(2026, 6, 1))
                .build();

        var metadata = AuditMetadataBuilder.build(
                AuditEventType.TRANSACTION_CREATE,
                new Object[] {},
                ResponseEntity.ok(transaction),
                null
        );

        assertThat(metadata).containsEntry("entityId", 10L);
        assertThat(metadata).containsEntry("amount", new BigDecimal("1500.00"));
        assertThat(metadata).containsEntry("category", "Rent");
    }

    @Test
    @DisplayName("builds AI chat metadata with message hash only")
    void buildsAiChatMetadata() {
        AIAccountantChatRequest request = new AIAccountantChatRequest();
        request.setMessage("What are my expenses?");

        var metadata = AuditMetadataBuilder.build(
                AuditEventType.AI_ACCOUNTANT_CHAT,
                new Object[] { request },
                AIAccountantChatResponse.builder().reply("Summary reply").build(),
                null
        );

        assertThat(metadata).containsKey("messageHash");
        assertThat(metadata).containsEntry("messageLength", 21);
        assertThat(metadata).containsEntry("replyLength", 13);
        assertThat(metadata).doesNotContainKey("message");
        assertThat(metadata).doesNotContainKey("reply");
    }

    @Test
    @DisplayName("builds import metadata with job stats")
    void buildsImportMetadata() {
        ImportJobResponse job = ImportJobResponse.builder()
                .id(5L)
                .fileName("statement.csv")
                .fileSize(1024L)
                .rowsImported(20)
                .errorsCount(1)
                .bankFormat("MONOBANK")
                .build();

        var metadata = AuditMetadataBuilder.build(
                AuditEventType.IMPORT_UPLOAD,
                new Object[] {},
                ResponseEntity.ok(job),
                null
        );

        assertThat(metadata).containsEntry("jobId", 5L);
        assertThat(metadata).containsEntry("rowsImported", 20);
        assertThat(metadata).containsEntry("bankFormat", "MONOBANK");
    }
}
