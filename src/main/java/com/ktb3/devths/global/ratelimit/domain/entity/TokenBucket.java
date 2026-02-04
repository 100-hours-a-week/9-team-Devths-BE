package com.ktb3.devths.global.ratelimit.domain.entity;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

/**
 * 토큰 버킷 (버스트 불허)
 * 소비된 토큰 개수를 AtomicInteger로 추적
 */
@Getter
public class TokenBucket {

	private final AtomicInteger consumedTokens;
	private final LocalDateTime createdAt;

	public TokenBucket() {
		this.consumedTokens = new AtomicInteger(0);
		this.createdAt = LocalDateTime.now();
	}

	/**
	 * 토큰 1개 소비 시도
	 *
	 * @param capacity 버킷 용량 (일일 제한)
	 * @return 소비 성공 여부
	 */
	public boolean tryConsumeToken(int capacity) {
		int currentCount = consumedTokens.incrementAndGet();
		return currentCount <= capacity;
	}

	/**
	 * 현재 소비된 토큰 개수
	 */
	public int getConsumedCount() {
		return consumedTokens.get();
	}

	/**
	 * 남은 토큰 개수
	 *
	 * @param capacity 버킷 용량
	 */
	public int getRemainingTokens(int capacity) {
		return Math.max(0, capacity - consumedTokens.get());
	}

	/**
	 * 토큰 리필 (테스트용)
	 */
	public void refill() {
		consumedTokens.set(0);
	}
}
