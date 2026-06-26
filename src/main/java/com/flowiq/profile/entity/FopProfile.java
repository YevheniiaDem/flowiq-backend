package com.flowiq.profile.entity;

import com.flowiq.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fop_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FopProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 1–3 for FOP groups, 0 for general tax system. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "fop_group", nullable = false)
    private int fopGroup = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_system", nullable = false, length = 30)
    private TaxSystem taxSystem = TaxSystem.SINGLE_TAX;

    @Column(name = "vat_payer", nullable = false)
    private boolean vatPayer = false;

    @Column(name = "tax_rate", precision = 6, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(length = 100)
    private String region;

    @Column(name = "main_kved", length = 20)
    private String mainKved;

    @Column(name = "main_kved_name", length = 255)
    private String mainKvedName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kved_codes", nullable = false, columnDefinition = "jsonb")
    private List<String> kvedCodes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
