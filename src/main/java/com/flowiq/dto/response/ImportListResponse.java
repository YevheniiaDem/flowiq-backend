package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "List of import jobs with statistics")
public class ImportListResponse {

    private List<ImportJobResponse> jobs;
    private ImportStatsResponse stats;
}
