package com.flowiq.profile.dto.request;

import com.flowiq.profile.entity.TaxSystem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Update FOP profile fields")
public class UpdateFopProfileRequest {

    @Min(0)
    @Max(3)
    @Schema(description = "FOP group: 1, 2, 3, or 0 for general tax system", example = "2")
    private int fopGroup;

    @NotNull
    private TaxSystem taxSystem;

    private boolean vatPayer;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Schema(example = "0.05")
    private BigDecimal taxRate;

    private LocalDate registrationDate;

    @Size(max = 100)
    @Schema(example = "Київська область")
    private String region;

    @Size(max = 20)
    @Schema(example = "62.01")
    private String mainKved;

    @Size(max = 255)
    @Schema(example = "Комп'ютерне програмування")
    private String mainKvedName;

    @NotNull
    private List<@NotBlank @Size(max = 20) String> kvedCodes;
}
