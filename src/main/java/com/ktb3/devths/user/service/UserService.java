package com.ktb3.devths.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.auth.dto.internal.TokenPair;
import com.ktb3.devths.auth.service.JwtTokenService;
import com.ktb3.devths.auth.service.TokenEncryptionService;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.security.jwt.JwtTokenProvider;
import com.ktb3.devths.global.security.jwt.JwtTokenValidator;
import com.ktb3.devths.user.domain.Interests;
import com.ktb3.devths.user.domain.UserRoles;
import com.ktb3.devths.user.domain.entity.SocialAccount;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.domain.entity.UserInterest;
import com.ktb3.devths.user.dto.internal.UserSignupResult;
import com.ktb3.devths.user.dto.request.UserSignupRequest;
import com.ktb3.devths.user.dto.response.UserSignupResponse;
import com.ktb3.devths.user.repository.SocialAccountRepository;
import com.ktb3.devths.user.repository.UserInterestRepository;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
	private static final String PROVIDER_GOOGLE = "GOOGLE";

	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final UserInterestRepository userInterestRepository;
	private final JwtTokenValidator jwtTokenValidator;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtTokenService jwtTokenService;
	private final TokenEncryptionService tokenEncryptionService;

	@Transactional
	public UserSignupResult signup(UserSignupRequest request) {
		jwtTokenValidator.validateTempToken(request.tempToken());

		String emailFromToken = jwtTokenProvider.getEmailFromToken(request.tempToken());
		if (!emailFromToken.equals(request.email())) {
			throw new CustomException(ErrorCode.INVALID_TEMP_TOKEN);
		}

		String provider = jwtTokenProvider.getProviderFromTempToken(request.tempToken());
		String googleSub = jwtTokenProvider.getGoogleSubFromTempToken(request.tempToken());
		String googleAccessToken = jwtTokenProvider.getGoogleAccessTokenFromTempToken(request.tempToken());
		String googleRefreshToken = jwtTokenProvider.getGoogleRefreshTokenFromTempToken(request.tempToken());
		int googleAccessTokenExpiresIn = jwtTokenProvider.getGoogleAccessTokenExpiresInFromTempToken(
			request.tempToken()
		);

		if (!PROVIDER_GOOGLE.equals(provider)) {
			throw new CustomException(ErrorCode.INVALID_TEMP_TOKEN);
		}

		if (userRepository.existsByEmail(request.email())) {
			throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
		}

		if (userRepository.existsByNickname(request.nickname())) {
			throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
		}

		if (socialAccountRepository.findByProviderAndProviderUserId(provider, googleSub).isPresent()) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		User user = userRepository.save(User.builder()
			.email(request.email())
			.nickname(request.nickname())
			.role(UserRoles.ROLE_USER)
			.isWithdraw(false)
			.build());

		String encryptedGoogleAccessToken = tokenEncryptionService.encrypt(googleAccessToken);
		// RT may be missing for first-time consent; store empty value to satisfy non-null column.
		String encryptedGoogleRefreshToken = googleRefreshToken == null
			? ""
			: tokenEncryptionService.encrypt(googleRefreshToken);
		LocalDateTime googleTokenExpiresAt = LocalDateTime.now().plusSeconds(googleAccessTokenExpiresIn);

		SocialAccount socialAccount = SocialAccount.builder()
			.user(user)
			.provider(provider)
			.providerUserId(googleSub)
			.accessToken(encryptedGoogleAccessToken)
			.refreshToken(encryptedGoogleRefreshToken)
			.expiresAt(googleTokenExpiresAt)
			.build();
		socialAccountRepository.save(socialAccount);

		List<UserInterest> interests = request.interests().stream()
			.map(this::parseInterest)
			.map(interest -> UserInterest.builder().user(user).interest(interest).build())
			.collect(Collectors.toList());

		try {
			// Duplicate interests are handled by DB unique constraints for now.
			userInterestRepository.saveAll(interests);
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		TokenPair tokenPair = jwtTokenService.issueTokenPair(user);

		List<String> interestNames = interests.stream()
			.map(userInterest -> userInterest.getInterest().name())
			.collect(Collectors.toList());

		UserSignupResponse response = UserSignupResponse.of(user, interestNames);

		log.info("회원가입 성공: userId={}", user.getId());

		return new UserSignupResult(response, tokenPair);
	}

	private Interests parseInterest(String value) {
		try {
			return Interests.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}
}
