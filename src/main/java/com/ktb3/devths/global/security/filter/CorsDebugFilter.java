package com.ktb3.devths.global.security.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ktb3.devths.global.util.LogSanitizer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CorsDebugFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String method = request.getMethod();
		String uri = LogSanitizer.sanitizeUri(request.getRequestURI());
		String origin = request.getHeader("Origin");

		// 요청 로깅
		log.info("=== CORS Debug - Request ===");
		log.info("Method: {}, URI: {}", method, uri);
		log.info("Origin: {}", LogSanitizer.sanitize(origin));

		if ("OPTIONS".equals(method)) {
			// Preflight 요청
			String requestMethod = request.getHeader("Access-Control-Request-Method");
			String requestHeaders = request.getHeader("Access-Control-Request-Headers");
			log.info("Preflight - Request-Method: {}, Request-Headers: {}",
				LogSanitizer.sanitize(requestMethod),
				LogSanitizer.sanitize(requestHeaders));
		}

		// 필터 체인 실행
		filterChain.doFilter(request, response);

		// 응답 로깅
		log.info("=== CORS Debug - Response ===");
		log.info("Status: {}", response.getStatus());
		log.info("Access-Control-Allow-Origin: {}",
			LogSanitizer.sanitize(response.getHeader("Access-Control-Allow-Origin")));
		log.info("Access-Control-Allow-Methods: {}",
			LogSanitizer.sanitize(response.getHeader("Access-Control-Allow-Methods")));
		log.info("Access-Control-Allow-Headers: {}",
			LogSanitizer.sanitize(response.getHeader("Access-Control-Allow-Headers")));
		log.info("Access-Control-Allow-Credentials: {}",
			response.getHeader("Access-Control-Allow-Credentials"));
		log.info("===========================");
	}
}
