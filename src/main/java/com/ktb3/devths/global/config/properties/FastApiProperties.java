package com.ktb3.devths.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fastapi")
public class FastApiProperties {
	private String baseUrl;
	private int timeout = 30000;
	private int pollInterval = 2000;
	private int maxPollAttempts = 150;
}
