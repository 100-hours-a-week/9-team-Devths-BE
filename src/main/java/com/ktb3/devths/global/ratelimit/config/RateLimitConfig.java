package com.ktb3.devths.global.ratelimit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Rate Limit 설정
 * - @EnableScheduling: 자정 리셋을 위한 스케줄링 활성화
 */
@Configuration
@EnableScheduling
public class RateLimitConfig {
	// InMemoryRateLimitService가 @Service로 자동 등록되므로 별도 Bean 설정 불필요
	// 추후 Redis 전환 시 @ConditionalOnProperty로 구현체 선택 가능
}
