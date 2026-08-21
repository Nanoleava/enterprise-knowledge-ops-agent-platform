package com.ljl.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<Long> fixedWindowRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(
                "redis/fixed-window-rate-limit.lua"
        ));
        script.setResultType(Long.class);
        return script;
    }
}
