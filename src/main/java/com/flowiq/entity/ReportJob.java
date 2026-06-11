package com.flowiq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Format format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_content")
    private byte[] fileContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ReportType {
        PROFIT_AND_LOSS,
        CASH_FLOW,
        REVENUE_SUMMARY,
        EXPENSE_SUMMARY,
        TAX_SUMMARY,
        FOP_SUMMARY
    }

    public enum Format {
        PDF,
        CSV,
        EXCEL
    }

    public enum Status {
        PENDING,
        GENERATING,
        COMPLETED,
        FAILED
    }
}
