package com.ktb3.devths.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.auth.dto.internal.GoogleIdTokenPayload;
import com.ktb3.devths.auth.dto.internal.GoogleLoginResult;
import com.ktb3.devths.auth.dto.internal.GoogleTokenResponse;
import com.ktb3.devths.auth.dto.internal.TokenPair;
import com.ktb3.devths.auth.dto.response.LoginResponse;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.security.jwt.JwtTokenProvider;
import com.ktb3.devths.user.domain.Interests;
import com.ktb3.devths.user.domain.entity.SocialAccount;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.SocialAccountRepository;
import com.ktb3.devths.user.repository.UserInterestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
	private static final String PROVIDER_GOOGLE = "GOOGLE";

	private final GoogleOAuthService googleOAuthService;
	private final TokenEncryptionService tokenEncryptionService;
	private final JwtTokenService jwtTokenService;
	private final JwtTokenProvider jwtTokenProvider;
	private final SocialAccountRepository socialAccountRepository;
	private final UserInterestRepository userInterestRepository;

	/**
	 * Google OAuth2 로그인
	 *
	 * @param authCode Google Authorization Code
	 * @return GoogleLoginResult (existing: LoginResponse + TokenPair, new: tempToken)
	 */
	@Transactional
	public GoogleLoginResult loginWithGoogle(String authCode) {
		// 1. authCode → Google Token 교환
		GoogleTokenResponse googleTokenResponse = googleOAuthService.exchangeAuthCodeForToken(authCode);

		// 2. idToken 검증 → Google sub, email 추출
		GoogleIdTokenPayload idTokenPayload = googleOAuthService.verifyIdToken(googleTokenResponse.idToken());

		// 3. DB에서 SocialAccount 조회
		SocialAccount socialAccount = socialAccountRepository
			.findByProviderAndProviderUserId(PROVIDER_GOOGLE, idTokenPayload.sub())
			.orElse(null);

		if (socialAccount == null) {
			String tempToken = jwtTokenProvider.generateTempToken(
				idTokenPayload.email(),
				idTokenPayload.sub(),
				PROVIDER_GOOGLE,
				googleTokenResponse.accessToken(),
				googleTokenResponse.refreshToken(),
				googleTokenResponse.expiresIn()
			);

			log.warn("신규 사용자 로그인 시도: sub={}", maskSub(idTokenPayload.sub()));
			return GoogleLoginResult.newUser(idTokenPayload.email(), tempToken);
		}

		User user = socialAccount.getUser();

		// 4. 탈퇴 회원 확인
		if (user.isWithdraw()) {
			log.warn("탈퇴한 회원 로그인 시도: userId={}", user.getId());
			throw new CustomException(ErrorCode.WITHDRAWN_USER);
		}

		// 5. Google AT/RT 갱신 (암호화 후 SocialAccount 업데이트)
		String encryptedGoogleAccessToken = tokenEncryptionService.encrypt(googleTokenResponse.accessToken());
		String encryptedGoogleRefreshToken = googleTokenResponse.refreshToken() != null
			? tokenEncryptionService.encrypt(googleTokenResponse.refreshToken())
			: socialAccount.getRefreshToken(); // Google RT가 없으면 기존 RT 유지

		LocalDateTime googleTokenExpiresAt = LocalDateTime.now()
			.plusSeconds(googleTokenResponse.expiresIn());

		socialAccount.updateTokens(encryptedGoogleAccessToken, encryptedGoogleRefreshToken, googleTokenExpiresAt);

		// 6. 서비스 AT/RT 발급
		TokenPair tokenPair = jwtTokenService.issueTokenPair(user);

		// 7. UserInterest 조회
		List<Interests> interests = userInterestRepository.findInterestsByUserId(user.getId());
		List<String> interestNames = interests.stream()
			.map(Enum::name)
			.collect(Collectors.toList());

		// 8. 로그인 결과 반환
		LoginResponse loginResponse = LoginResponse.of(user, interestNames);

		log.info("로그인 성공: userId={}, email={}", user.getId(), maskEmail(user.getEmail()));

		return GoogleLoginResult.registered(loginResponse, tokenPair);
	}

	/**
	 * 로그아웃
	 *
	 * @param userId 사용자 ID
	 */
	@Transactional
	public void logout(Long userId) {
		jwtTokenService.invalidateRefreshToken(userId);
		log.info("로그아웃 성공: userId={}", userId);
	}

	/**
	 * 이메일 마스킹 (로그용)
	 */
	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return "****";
		}
		String[] parts = email.split("@");
		return parts[0].substring(0, Math.min(2, parts[0].length())) + "****@" + parts[1];
	}

	/**
	 * Google sub 마스킹 (로그용)
	 */
	private String maskSub(String sub) {
		if (sub == null || sub.length() < 4) {
			return "****";
		}
		return "****" + sub.substring(sub.length() - 4);
	}
}
