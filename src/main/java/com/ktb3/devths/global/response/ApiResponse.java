package com.ktb3.devths.global.response;

import java.time.LocalDateTime;

public record ApiResponse<T>(
	String message,
	T data,
	LocalDateTime timestamp
) {
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(message, data, LocalDateTime.now());
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>("요청이 성공적으로 처리되었습니다", data, LocalDateTime.now());
	}

	public static ApiResponse<Void> error(String message) {
		return new ApiResponse<>(message, null, LocalDateTime.now());
	}

	public static ApiResponse<Void> error(ErrorCode errorCode) {
		return new ApiResponse<>(errorCode.getMessage(), null, LocalDateTime.now());
	}
}
