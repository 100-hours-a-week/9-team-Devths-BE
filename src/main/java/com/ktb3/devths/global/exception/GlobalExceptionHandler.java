package com.ktb3.devths.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException ex) {
		log.error("CustomException: {}", ex.getMessage());
		ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
		log.error("Unhandled Exception: ", ex);
		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
