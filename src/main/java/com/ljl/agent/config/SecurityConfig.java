package com.ljl.agent.config;

import com.ljl.agent.security.JsonAccessDeniedHandler;
import com.ljl.agent.security.JsonAuthenticationEntryPoint;
import com.ljl.agent.security.JwtAuthorityConverter;
import com.ljl.agent.security.ProjectUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.time.Clock;

/**
 * 阶段 3 Day 1 HTTP 安全边界和官方 Resource Server JWT 配置。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final Environment environment;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            Environment environment,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler
    ) {
        this.environment = environment;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public Clock jwtClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey jwtSecretKey(SecurityProperties properties) {
        return properties.createSecretKey();
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            SecurityProperties properties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(properties.getIssuer())
        );
        return decoder;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            ProjectUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new JwtAuthorityConverter()
        );
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter
                        ))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(HttpMethod.GET, "/api/health")
                            .permitAll()
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/api/users/register",
                                    "/api/auth/login"
                            )
                            .permitAll();

                    if (environment.acceptsProfiles(Profiles.of("dev"))) {
                        authorize.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll();
                    }

                    authorize
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/api/users",
                                    "/api/users/**"
                            )
                            .hasRole("ADMIN")
                            .requestMatchers("/api/**")
                            .authenticated()
                            .anyRequest()
                            .denyAll();
                });

        return http.build();
    }
}
