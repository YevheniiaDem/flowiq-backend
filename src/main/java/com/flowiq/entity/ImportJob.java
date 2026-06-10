package com.flowiq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "rows_processed", nullable = false)
    private int rowsProcessed;

    @Column(name = "rows_imported", nullable = false)
    private int rowsImported;

    @Column(name = "errors_count", nullable = false)
    private int errorsCount;

    @Column(name = "bank_format", length = 50)
    private String bankFormat;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        PARTIAL,
        FAILED
    }
}
