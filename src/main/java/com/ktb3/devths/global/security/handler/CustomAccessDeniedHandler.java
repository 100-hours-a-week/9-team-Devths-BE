package com.ktb3.devths.global.security.handler;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.response.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
	private final ObjectMapper objectMapper;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException {

		log.warn("접근 권한 없음 - URI: {}, IP: {}", request.getRequestURI(), request.getRemoteAddr());

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Void> errorResponse = ApiResponse.error(ErrorCode.ACCESS_DENIED);

		String jsonResponse = objectMapper.writeValueAsString(errorResponse);
		response.getWriter().write(jsonResponse);
	}
}
