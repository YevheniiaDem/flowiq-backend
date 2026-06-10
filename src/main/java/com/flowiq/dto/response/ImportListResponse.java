package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportListResponse {

    private List<ImportJobResponse> jobs;
    private ImportStatsResponse stats;
}
