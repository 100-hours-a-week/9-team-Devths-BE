package com.ktb3.devths.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.ktb3.devths.global.config.properties.JwtProperties;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtTokenProvider {
	private static final String USER_ID_CLAIM = "userId";
	private static final String EMAIL_CLAIM = "email";
	private static final String ROLE_CLAIM = "role";
	private static final String GOOGLE_SUB_CLAIM = "googleSub";
	private static final String PROVIDER_CLAIM = "provider";

	private final JwtProperties jwtProperties;

	public String generateAccessToken(Long userId, String email, String role) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

		SecretKey key = getSigningKey();

		return Jwts.builder()
			.claim(USER_ID_CLAIM, userId)
			.claim(EMAIL_CLAIM, email)
			.claim(ROLE_CLAIM, role)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(key)
			.compact();
	}

	public String generateRefreshToken(Long userId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

		SecretKey key = getSigningKey();

		return Jwts.builder()
			.claim(USER_ID_CLAIM, userId)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(key)
			.compact();
	}

	public String generateTempToken(String email, String googleSub, String provider) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getTempTokenExpiration());

		SecretKey key = getSigningKey();

		return Jwts.builder()
			.claim(EMAIL_CLAIM, email)
			.claim(GOOGLE_SUB_CLAIM, googleSub)
			.claim(PROVIDER_CLAIM, provider)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(key)
			.compact();
	}

	public Claims parseToken(String token) {
		try {
			SecretKey key = getSigningKey();
			return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (JwtException e) {
			log.warn("JWT 파싱 실패: {}", e.getMessage());
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	public Long getUserIdFromToken(String token) {
		Claims claims = parseToken(token);
		Object userIdObj = claims.get(USER_ID_CLAIM);

		if (userIdObj == null) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		if (userIdObj instanceof Integer) {
			return ((Integer)userIdObj).longValue();
		} else if (userIdObj instanceof Long) {
			return (Long)userIdObj;
		} else {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	public String getEmailFromToken(String token) {
		Claims claims = parseToken(token);
		String email = claims.get(EMAIL_CLAIM, String.class);

		if (email == null) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		return email;
	}

	public String getRoleFromToken(String token) {
		Claims claims = parseToken(token);
		String role = claims.get(ROLE_CLAIM, String.class);

		if (role == null) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		return role;
	}

	public String getGoogleSubFromTempToken(String token) {
		Claims claims = parseToken(token);
		String googleSub = claims.get(GOOGLE_SUB_CLAIM, String.class);

		if (googleSub == null) {
			throw new CustomException(ErrorCode.INVALID_TEMP_TOKEN);
		}

		return googleSub;
	}

	public String getProviderFromTempToken(String token) {
		Claims claims = parseToken(token);
		String provider = claims.get(PROVIDER_CLAIM, String.class);

		if (provider == null) {
			throw new CustomException(ErrorCode.INVALID_TEMP_TOKEN);
		}

		return provider;
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
