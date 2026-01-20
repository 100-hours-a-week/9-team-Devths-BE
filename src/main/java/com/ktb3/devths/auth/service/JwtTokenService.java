package com.ktb3.devths.auth.service;

import java.time.LocalDateTime;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.auth.dto.internal.TokenPair;
import com.ktb3.devths.global.config.properties.JwtProperties;
import com.ktb3.devths.global.security.jwt.JwtTokenProvider;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.domain.entity.UserToken;
import com.ktb3.devths.user.repository.UserTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtTokenService {
	private final JwtTokenProvider jwtTokenProvider;
	private final UserTokenRepository userTokenRepository;
	private final JwtProperties jwtProperties;

	/**
	 * 서비스 Access Token / Refresh Token 발급 및 저장
	 *
	 * @param user 사용자 엔티티
	 * @return TokenPair (accessToken, refreshToken, refreshTokenExpiresAt)
	 */
	@Transactional
	public TokenPair issueTokenPair(User user) {
		// Access Token 발급
		String accessToken = jwtTokenProvider.generateAccessToken(
			user.getId(),
			user.getEmail(),
			user.getRole().name()
		);

		// Refresh Token 발급
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

		// Refresh Token 만료 시간 계산
		LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
			.plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000);

		// UserToken 저장
		UserToken userToken = UserToken.builder()
			.user(user)
			.refreshToken(refreshToken)
			.expiresAt(refreshTokenExpiresAt)
			.build();
		userTokenRepository.save(userToken);

		log.info("토큰 발급 완료: userId={}", user.getId());

		return new TokenPair(accessToken, refreshToken, refreshTokenExpiresAt);
	}

	/**
	 * Refresh Token 무효화 (로그아웃)
	 *
	 * @param userId 사용자 ID
	 */
	@Transactional
	public void invalidateRefreshToken(Long userId) {
		User user = User.builder().id(userId).build();
		userTokenRepository.deleteByUserId(user.getId());

		log.info("Refresh Token 무효화 완료: userId={}", userId);
	}
}
