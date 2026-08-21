package com.ljl.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+",
        disabledReason = "未提供 DB_PASSWORD，跳过需要完整应用上下文的 OpenAPI 测试"
)
class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldExposeCoreOpenApiContractAndSwaggerUi() throws Exception {
        HttpResponse<String> defaultApiDocs = get("/v3/api-docs");
        HttpResponse<String> apiDocs = get(
                "/v3/api-docs/stage-4-day-1"
        );

        assertEquals(200, defaultApiDocs.statusCode());
        assertEquals(200, apiDocs.statusCode());
        assertTrue(apiDocs.body().contains(
                "\"title\":\"LJL Java Agent Backend API\""
        ));
        assertCorePath(apiDocs.body(), "/api/auth/login");
        assertCorePath(apiDocs.body(), "/api/auth/logout");
        assertCorePath(apiDocs.body(), "/api/users/me");
        assertTrue(!apiDocs.body().contains("/api/users/login"));
        assertTrue(apiDocs.body().contains("\"bearerAuth\""));
        assertTrue(apiDocs.body().contains("\"scheme\":\"bearer\""));
        assertCorePath(apiDocs.body(), "/api/knowledge-bases/{id}");
        assertCorePath(apiDocs.body(), "/api/documents");
        assertCorePath(apiDocs.body(), "/api/documents/{id}");
        assertCorePath(apiDocs.body(), "/api/documents/{documentId}/chunks");
        assertCorePath(
                apiDocs.body(),
                "/api/knowledge-bases/{knowledgeBaseId}/documents/upload"
        );
        assertCorePath(apiDocs.body(), "/api/documents/{documentId}/parse");
        assertCorePath(
                apiDocs.body(),
                "/api/documents/{documentId}/processing-status"
        );
        assertCorePath(apiDocs.body(), "/api/chat/sessions");
        assertCorePath(
                apiDocs.body(),
                "/api/chat/sessions/{sessionId}/messages"
        );
        assertTrue(apiDocs.body().contains("\"400\""));
        assertTrue(apiDocs.body().contains("\"404\""));
        assertTrue(apiDocs.body().contains("\"409\""));
        assertTrue(apiDocs.body().contains("\"413\""));
        assertTrue(apiDocs.body().contains("\"422\""));
        assertTrue(apiDocs.body().contains("\"429\""));
        assertTrue(apiDocs.body().contains("\"503\""));

        HttpResponse<String> swaggerUi = get("/swagger-ui/index.html");
        assertEquals(200, swaggerUi.statusCode());
        assertTrue(swaggerUi.body().contains("Swagger UI"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private void assertCorePath(String openApiJson, String path) {
        assertTrue(
                openApiJson.contains("\"" + path + "\""),
                () -> "OpenAPI 缺少核心接口：" + path
        );
    }
}
