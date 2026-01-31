package com.ktb3.devths.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.google")
public class GoogleOAuthProperties {
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;
}
