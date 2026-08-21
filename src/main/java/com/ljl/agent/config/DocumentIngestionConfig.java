package com.ljl.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 注册文档摄取配置，不把它耦合到安全配置中。
 */
@Configuration
@EnableConfigurationProperties(DocumentIngestionProperties.class)
public class DocumentIngestionConfig {
}
