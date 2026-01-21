package com.ktb3.devths.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
	private final String secret;
	private final long accessTokenExpiration;
	private final long refreshTokenExpiration;
	private final long tempTokenExpiration;
}
