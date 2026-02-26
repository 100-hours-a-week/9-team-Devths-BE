package com.ktb3.devths.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "spring.security.oauth2.client.provider.google")
public class GoogleOAuthProviderProperties {
	private final String tokenUri;
	private final String tokenInfoUri;
}
