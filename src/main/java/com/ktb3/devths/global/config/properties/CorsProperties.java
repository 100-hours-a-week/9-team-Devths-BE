package com.ktb3.devths.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
	private final String allowedOrigins;
	private final String allowedMethods;
	private final String allowedHeaders;
	private final String exposedHeaders;
	private final boolean allowCredentials;
	private final long maxAge;
}
