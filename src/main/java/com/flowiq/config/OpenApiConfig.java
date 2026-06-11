package com.flowiq.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI flowiqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flowiq API")
                        .description("AI-powered accounting platform for Ukrainian FOPs and small businesses.")
                        .version("v1")
                        .contact(new Contact().name("Flowiq Team"))
                        .license(new License().name("Internal Development")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token obtained from /api/auth/login or /api/auth/register")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public APIs")
                .pathsToMatch(
                        "/api/health",
                        "/api/health/**",
                        "/api/auth/register",
                        "/api/auth/login"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi protectedApi() {
        return GroupedOpenApi.builder()
                .group("protected")
                .displayName("Protected APIs")
                .pathsToMatch(
                        "/api/dashboard/**",
                        "/api/analytics/**",
                        "/api/transactions/**",
                        "/api/imports/**",
                        "/api/reports/**",
                        "/api/ai-accountant/**",
                        "/api/notifications/**"
                )
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                    return operation;
                })
                .build();
    }
}
