package com.ljl.agent.controller;

import com.ljl.agent.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最基础的健康检查接口。
 *
 * 用于确认：
 * 1. Spring Boot 已经启动；
 * 2. Controller 已经被扫描；
 * 3. HTTP 请求能够进入后端；
 * 4. Java 对象能够转换成 JSON。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "健康检查", description = "应用存活状态")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "检查应用是否正常运行")
    @SecurityRequirements
    public Result<String> health() {
        return Result.success("OK");
    }
}
