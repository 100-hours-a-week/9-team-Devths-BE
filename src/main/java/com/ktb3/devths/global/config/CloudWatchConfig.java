package com.ktb3.devths.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
public class CloudWatchConfig {

	@Value("${cloud.aws.region.static-region}")
	private String awsRegion;

	@Bean
	public CloudWatchAsyncClient cloudWatchAsyncClient() {
		return CloudWatchAsyncClient.builder()
			.region(Region.of(awsRegion))
			.build();
	}
}
