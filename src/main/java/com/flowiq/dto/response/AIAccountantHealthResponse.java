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
public class AIAccountantHealthResponse {

    private int score;
    private String status;
    private String summary;
    private List<String> highlights;
}
