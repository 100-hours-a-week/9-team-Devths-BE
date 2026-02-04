package com.ktb3.devths.global.ratelimit.service;

import com.ktb3.devths.global.ratelimit.domain.constant.ApiType;

/**
 * Rate Limit 서비스 인터페이스
 * 인메모리 또는 Redis 구현체로 교체 가능
 */
public interface RateLimitService {

	/**
	 * 토큰 소비 (없으면 예외 발생)
	 *
	 * @param userId 사용자 ID
	 * @param apiType API 종류
	 * @throws com.ktb3.devths.global.ratelimit.exception.RateLimitExceededException 제한 초과 시
	 */
	void consumeToken(Long userId, ApiType apiType);

	/**
	 * 현재 사용한 토큰 개수 조회
	 *
	 * @param userId 사용자 ID
	 * @param apiType API 종류
	 * @return 현재 사용한 토큰 개수
	 */
	int getConsumedCount(Long userId, ApiType apiType);

	/**
	 * 남은 토큰 개수 조회
	 *
	 * @param userId 사용자 ID
	 * @param apiType API 종류
	 * @return 남은 토큰 개수
	 */
	int getRemainingTokens(Long userId, ApiType apiType);

	/**
	 * 토큰 리필 (테스트용)
	 *
	 * @param userId 사용자 ID
	 * @param apiType API 종류
	 */
	void refillTokens(Long userId, ApiType apiType);
}
