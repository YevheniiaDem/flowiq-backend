package com.flowiq.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.AIAccountantHealthResponse;
import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.ForecastsResponse;
import com.flowiq.dto.response.TaxAdvisorResponse;
import com.flowiq.service.AIAccountantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI Accountant", description = "AI-powered accounting assistant, tax advisor, and forecasts")
@RestController
@RequestMapping("/api/ai-accountant")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AIAccountantController {

    private final AIAccountantService aiAccountantService;

    @Operation(summary = "AI Accountant health", description = "Returns the health status of the AI Accountant module.")
    @ApiResponse(responseCode = "200", description = "AI Accountant health status",
            content = @Content(schema = @Schema(implementation = AIAccountantHealthResponse.class)))
    @ApiErrorResponses
    @GetMapping("/health")
    public ResponseEntity<AIAccountantHealthResponse> getHealth() {
        return ResponseEntity.ok(aiAccountantService.getHealth());
    }

    @Operation(summary = "AI recommendations", description = "Returns personalized AI financial recommendations.")
    @ApiResponse(responseCode = "200", description = "List of AI recommendations",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AIRecommendationResponse.class))))
    @ApiErrorResponses
    @GetMapping("/recommendations")
    public ResponseEntity<List<AIRecommendationResponse>> getRecommendations() {
        return ResponseEntity.ok(aiAccountantService.getRecommendations());
    }

    @Operation(summary = "Tax advisor", description = "Returns AI-generated tax advice for Ukrainian FOPs.")
    @ApiResponse(responseCode = "200", description = "Tax advisor response",
            content = @Content(schema = @Schema(implementation = TaxAdvisorResponse.class)))
    @ApiErrorResponses
    @GetMapping("/tax-advisor")
    public ResponseEntity<TaxAdvisorResponse> getTaxAdvisor() {
        return ResponseEntity.ok(aiAccountantService.getTaxAdvisor());
    }

    @Operation(summary = "Financial forecasts", description = "Returns AI-generated revenue and expense forecasts.")
    @ApiResponse(responseCode = "200", description = "Forecasts data",
            content = @Content(schema = @Schema(implementation = ForecastsResponse.class)))
    @ApiErrorResponses
    @GetMapping("/forecasts")
    public ResponseEntity<ForecastsResponse> getForecasts() {
        return ResponseEntity.ok(aiAccountantService.getForecasts());
    }

    @Operation(summary = "Chat with AI Accountant", description = "Sends a message to the AI Accountant and receives a response.")
    @ApiResponse(responseCode = "200", description = "AI chat response",
            content = @Content(schema = @Schema(implementation = AIAccountantChatResponse.class)))
    @ApiErrorResponses
    @PostMapping("/chat")
    public ResponseEntity<AIAccountantChatResponse> chat(@Valid @RequestBody AIAccountantChatRequest request) {
        return ResponseEntity.ok(aiAccountantService.chat(request));
    }
}
