package com.ktb3.devths.global.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// 400 Bad Request
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
	GOOGLE_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, "Google 토큰 교환에 실패했습니다"),
	GOOGLE_USER_INFO_FETCH_FAILED(HttpStatus.BAD_REQUEST, "Google 사용자 정보 조회에 실패했습니다"),
	INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다"),

	// 401 Unauthorized
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다"),
	EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다"),
	INVALID_TEMP_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 임시 토큰입니다"),
	EXPIRED_TEMP_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 임시 토큰입니다"),
	REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "이미 사용된 리프레시 토큰입니다"),

	// 403 Forbidden
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
	WITHDRAWN_USER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다"),

	// 404 Not Found
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

	// 409 Conflict
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),

	// 500 Internal Server Error
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");

	private final HttpStatus status;
	private final String message;
}
