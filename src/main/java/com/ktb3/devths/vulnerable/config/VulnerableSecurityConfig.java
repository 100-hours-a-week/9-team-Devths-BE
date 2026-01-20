package com.ktb3.devths.vulnerable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * INTENTIONALLY VULNERABLE SECURITY CONFIGURATION FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Configuration
@EnableWebSecurity
public class VulnerableSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 취약점 #1: CSRF 보호 완전히 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 취약점 #2: 모든 요청 허용
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 취약점 #3: 프레임 옵션 비활성화 (Clickjacking 취약)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(""))
                )

                // 취약점 #4: HTTP Basic 인증 사용 (암호화되지 않은 전송)
                .httpBasic(basic -> {});

        return http.build();
    }

    // 취약점 #5: 비밀번호 암호화 없음
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // 취약점 #6: 너무 관대한 CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 모든 origin 허용
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));

        // 모든 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(Collections.singletonList("*"));

        // 인증 정보 허용
        configuration.setAllowCredentials(true);

        // 모든 노출 헤더 허용
        configuration.setExposedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
