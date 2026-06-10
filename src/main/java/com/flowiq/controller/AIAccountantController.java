package com.flowiq.controller;

import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.AIAccountantHealthResponse;
import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.ForecastsResponse;
import com.flowiq.dto.response.TaxAdvisorResponse;
import com.flowiq.service.AIAccountantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-accountant")
@RequiredArgsConstructor
public class AIAccountantController {

    private final AIAccountantService aiAccountantService;

    @GetMapping("/health")
    public ResponseEntity<AIAccountantHealthResponse> getHealth() {
        return ResponseEntity.ok(aiAccountantService.getHealth());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<AIRecommendationResponse>> getRecommendations() {
        return ResponseEntity.ok(aiAccountantService.getRecommendations());
    }

    @GetMapping("/tax-advisor")
    public ResponseEntity<TaxAdvisorResponse> getTaxAdvisor() {
        return ResponseEntity.ok(aiAccountantService.getTaxAdvisor());
    }

    @GetMapping("/forecasts")
    public ResponseEntity<ForecastsResponse> getForecasts() {
        return ResponseEntity.ok(aiAccountantService.getForecasts());
    }

    @PostMapping("/chat")
    public ResponseEntity<AIAccountantChatResponse> chat(@Valid @RequestBody AIAccountantChatRequest request) {
        return ResponseEntity.ok(aiAccountantService.chat(request));
    }
}
