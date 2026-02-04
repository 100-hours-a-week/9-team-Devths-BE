package com.ktb3.devths.global.ratelimit.exception;

import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.ratelimit.domain.constant.ApiType;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends CustomException {

	private final ApiType apiType;
	private final int limit;

	public RateLimitExceededException(ApiType apiType, int limit) {
		super(ErrorCode.RATE_LIMIT_EXCEEDED);
		this.apiType = apiType;
		this.limit = limit;
	}

	@Override
	public String getMessage() {
		return String.format("%s의 일일 호출 제한(%d회)을 초과했습니다.",
			apiType.getDescription(), limit);
	}
}
