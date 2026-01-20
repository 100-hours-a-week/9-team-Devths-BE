package com.ktb3.devths.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ktb3.devths.global.config.properties.CorsProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig {
	private final CorsProperties corsProperties;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		List<String> origins = Arrays.asList(corsProperties.getAllowedOrigins().split(","));
		configuration.setAllowedOrigins(origins);

		List<String> methods = Arrays.asList(corsProperties.getAllowedMethods().split(","));
		configuration.setAllowedMethods(methods);

		if ("*".equals(corsProperties.getAllowedHeaders())) {
			configuration.addAllowedHeader("*");
		} else {
			List<String> headers = Arrays.asList(corsProperties.getAllowedHeaders().split(","));
			configuration.setAllowedHeaders(headers);
		}

		List<String> exposedHeaders = Arrays.asList(corsProperties.getExposedHeaders().split(","));
		configuration.setExposedHeaders(exposedHeaders);

		configuration.setAllowCredentials(corsProperties.isAllowCredentials());
		configuration.setMaxAge(corsProperties.getMaxAge());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
