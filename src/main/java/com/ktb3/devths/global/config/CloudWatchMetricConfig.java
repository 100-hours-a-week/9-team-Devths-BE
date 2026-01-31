package com.ktb3.devths.global.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Profile("!local")
@Slf4j
@Configuration
/**
 * 프로메테우스 반영 전까지 임시로 사용할 Config 입니다.
 */
public class CloudWatchMetricConfig {

	@Value("${cloud.aws.region.static-region}")
	private String awsRegion;

	@Value("${management.metrics.export.cloudwatch.namespace:devths-local}")
	private String namespace;

	@Value("${management.metrics.export.cloudwatch.step:60s}")
	private Duration step;

	@Value("${management.metrics.export.cloudwatch.batch-size:20}")
	private int batchSize;

	@Bean
	public CloudWatchAsyncClient cloudWatchAsyncClient() {

		try {
			/// EC2 Role로 작성하게 설정
			DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

			CloudWatchAsyncClient client = CloudWatchAsyncClient.builder()
				.region(Region.of(awsRegion))
				.credentialsProvider(credentialsProvider)
				.build();

			return client;
		} catch (Exception e) {
			throw e;
		}
	}

	/// CloudWatchMeterRegistry 생성을 위해 설정
	@Bean
	public io.micrometer.cloudwatch2.CloudWatchConfig cloudWatchConfig() {
		return new io.micrometer.cloudwatch2.CloudWatchConfig() {
			@Override
			public String namespace() {
				return namespace;
			}

			@Override
			public Duration step() {
				return step;
			}

			@Override
			public int batchSize() {
				return batchSize;
			}

			@Override
			public String get(String key) {
				return null;
			}
		};
	}

	/// CloudWatchMeterRegistry 설정
	@Bean
	public CloudWatchMeterRegistry cloudWatchMeterRegistry(
		io.micrometer.cloudwatch2.CloudWatchConfig cloudWatchConfig,
		CloudWatchAsyncClient cloudWatchAsyncClient,
		Clock clock) {

		CloudWatchMeterRegistry registry = new CloudWatchMeterRegistry(
			cloudWatchConfig,
			clock,
			cloudWatchAsyncClient
		);

		// EC2 인스턴스 ID를 메트릭 Dimension에 추가
		String instanceId = getEc2InstanceId();

		if (instanceId != null) {
			registry.config().commonTags("InstanceId", instanceId);
		}
		return registry;
	}

	/**
	 * EC2 Instance Metadata에서 Instance ID 가져오기
	 * EC2가 아닌 경우 null 반환
	 */
	private String getEc2InstanceId() {
		try {
			// EC2 Instance Metadata Service v2 (IMDSv2) 사용
			String tokenUrl = "http://169.254.169.254/latest/api/token";
			String metadataUrl = "http://169.254.169.254/latest/meta-data/instance-id";

			// 1. IMDSv2 토큰 발급 (1초 타임아웃)
			java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
				.connectTimeout(java.time.Duration.ofSeconds(1))
				.build();

			java.net.http.HttpRequest tokenRequest = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(tokenUrl))
				.timeout(java.time.Duration.ofSeconds(1))
				.header("X-aws-ec2-metadata-token-ttl-seconds", "21600")
				.PUT(java.net.http.HttpRequest.BodyPublishers.ofString(""))
				.build();

			String token = client.send(tokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString()).body();

			// 2. 토큰을 사용하여 Instance ID 조회
			java.net.http.HttpRequest metadataRequest = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(metadataUrl))
				.timeout(java.time.Duration.ofSeconds(1))
				.header("X-aws-ec2-metadata-token", token)
				.GET()
				.build();

			String instanceId = client.send(metadataRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
				.body()
				.trim();

			return instanceId;
		} catch (Exception e) {
			log.warn("AWS EC2 메타데이터를 가져올 수 없습니다. 일반 환경으로 간주합니다. (사유: {})", e.getMessage());
			return null;
		}
	}

	@Bean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}
}
