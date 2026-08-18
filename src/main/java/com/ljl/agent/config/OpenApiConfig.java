package com.ljl.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agentBackendOpenApi() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("LJL Java Agent Backend API")
                        .version("stage-3-day-1")
                        .description(
                                "阶段 3 Day 1 REST API：Spring Security + Bearer JWT 认证与接口级授权。"
                        )
                        .contact(new Contact().name("LJL")))
                .components(new Components().addSecuritySchemes(
                        schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }

    @Bean
    public GroupedOpenApi stageThreeDayOneApi() {
        return GroupedOpenApi.builder()
                .group("stage-3-day-1")
                .pathsToMatch("/api/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    ApiResponses responses = operation.getResponses();
                    addResponseIfMissing(responses, "200", "请求成功，响应体使用 Result 结构");
                    addResponseIfMissing(responses, "400", "参数格式或参数校验失败");
                    addResponseIfMissing(responses, "401", "账号密码错误、缺少或无效 Bearer JWT");
                    addResponseIfMissing(responses, "403", "身份有效但角色权限不足，或业务资源归属校验失败");
                    addResponseIfMissing(responses, "404", "请求的业务资源不存在");
                    addResponseIfMissing(responses, "409", "唯一约束或幂等键冲突");
                    addResponseIfMissing(responses, "500", "系统内部错误");
                    return operation;
                })
                .build();
    }

    private void addResponseIfMissing(
            ApiResponses responses,
            String status,
            String description
    ) {
        if (!responses.containsKey(status)) {
            responses.addApiResponse(
                    status,
                    new ApiResponse().description(description)
            );
        }
    }
}
