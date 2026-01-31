package com.ktb3.devths.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ktb3.devths.global.config.properties.AwsProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
@RequiredArgsConstructor
public class S3Config {
	private final AwsProperties awsProperties;

	@Bean
	public AmazonS3 amazonS3() {
		BasicAWSCredentials credentials = new BasicAWSCredentials(
			awsProperties.getCredentials().getAccessKey(),
			awsProperties.getCredentials().getSecretKey()
		);

		return AmazonS3ClientBuilder.standard()
			.withCredentials(new AWSStaticCredentialsProvider(credentials))
			.withRegion(awsProperties.getRegion().getStaticRegion())
			.build();
	}
}
