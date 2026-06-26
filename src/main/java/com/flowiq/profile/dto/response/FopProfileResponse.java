package com.flowiq.profile.dto.response;

import com.flowiq.profile.entity.FopProfile;
import com.flowiq.profile.entity.TaxSystem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FOP business profile")
public class FopProfileResponse {

    private int fopGroup;
    private TaxSystem taxSystem;
    private boolean vatPayer;
    private BigDecimal taxRate;
    private LocalDate registrationDate;
    private String region;
    private String mainKved;
    private String mainKvedName;
    private List<String> kvedCodes;
    private String updatedAt;

    public static FopProfileResponse fromEntity(FopProfile profile) {
        return FopProfileResponse.builder()
                .fopGroup(profile.getFopGroup())
                .taxSystem(profile.getTaxSystem())
                .vatPayer(profile.isVatPayer())
                .taxRate(profile.getTaxRate())
                .registrationDate(profile.getRegistrationDate())
                .region(profile.getRegion())
                .mainKved(profile.getMainKved())
                .mainKvedName(profile.getMainKvedName())
                .kvedCodes(profile.getKvedCodes())
                .updatedAt(profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : null)
                .build();
    }
}
