package com.ktb3.devths.global.ratelimit.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiType {
	GOOGLE_CALENDAR("google.calendar", "Google Calendar API (쓰기 작업)"),
	GOOGLE_TASKS("google.tasks", "Google Tasks API (쓰기 작업)"),
	FASTAPI_ANALYSIS("fastapi.analysis", "FastAPI 분석 요청"),
	GOOGLE_OAUTH("google.oauth", "Google OAuth2 로그인");

	private final String key;
	private final String description;
}
