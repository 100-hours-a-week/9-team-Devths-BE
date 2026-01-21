package com.ktb3.devths.global.security.jwt;

import org.springframework.stereotype.Component;

import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenValidator {
	private final JwtTokenProvider jwtTokenProvider;

	public boolean validateAccessToken(String token) {
		try {
			jwtTokenProvider.parseToken(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("만료된 Access Token");
			throw new CustomException(ErrorCode.EXPIRED_TOKEN);
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("유효하지 않은 Access Token: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	public boolean validateTempToken(String token) {
		try {
			jwtTokenProvider.parseToken(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("만료된 Temp Token");
			throw new CustomException(ErrorCode.EXPIRED_TEMP_TOKEN);
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("유효하지 않은 Temp Token: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_TEMP_TOKEN);
		}
	}
}
