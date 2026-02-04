package com.ktb3.devths.global.ratelimit.domain.entity;

import java.time.LocalDate;
import java.util.Objects;

import com.ktb3.devths.global.ratelimit.domain.constant.ApiType;

/**
 * Rate Limit 저장소 키 (userId + apiType + date 조합)
 * 일일 제한을 위해 날짜 포함
 */
public record RateLimitKey(
	Long userId,
	ApiType apiType,
	LocalDate date
) {
	public RateLimitKey {
		Objects.requireNonNull(userId, "userId는 null일 수 없습니다");
		Objects.requireNonNull(apiType, "apiType은 null일 수 없습니다");
		Objects.requireNonNull(date, "date는 null일 수 없습니다");
	}

	public static RateLimitKey of(Long userId, ApiType apiType) {
		return new RateLimitKey(userId, apiType, LocalDate.now());
	}

	/**
	 * Redis 키 형식으로 변환
	 */
	@Override
	public String toString() {
		return String.format("ratelimit:%d:%s:%s", userId, apiType.getKey(), date);
	}
}
