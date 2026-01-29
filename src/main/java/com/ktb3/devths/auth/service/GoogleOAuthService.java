package com.ktb3.devths.auth.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.ktb3.devths.auth.dto.internal.GoogleIdTokenPayload;
import com.ktb3.devths.auth.dto.internal.GoogleTokenResponse;
import com.ktb3.devths.global.config.properties.GoogleOAuthProperties;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableConfigurationProperties(GoogleOAuthProperties.class)
@RequiredArgsConstructor
public class GoogleOAuthService {
	private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";

	private final GoogleOAuthProperties googleOAuthProperties;
	private final RestClient restClient;

	/**
	 * Google Authorization Code를 Access Token / Refresh Token / ID Token으로 교환
	 *
	 * @param authCode Google Authorization Code
	 * @return Google Token Response (accessToken, refreshToken, idToken, expiresIn)
	 */
	public GoogleTokenResponse exchangeAuthCodeForToken(String authCode) {
		try {
			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("code", authCode);
			params.add("client_id", googleOAuthProperties.getClientId());
			params.add("client_secret", googleOAuthProperties.getClientSecret());
			params.add("redirect_uri", googleOAuthProperties.getRedirectUri());
			params.add("grant_type", "authorization_code");

			GoogleTokenResponse response = restClient.post()
				.uri(GOOGLE_TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(params)
				.retrieve()
				.body(GoogleTokenResponse.class);

			if (response == null || response.idToken() == null) {
				log.error("Google 토큰 교환 실패: 응답이 null이거나 idToken이 없습니다");
				throw new CustomException(ErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
			}

			log.info("Google 토큰 교환 성공");
			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Google 토큰 교환 중 오류 발생", e);
			throw new CustomException(ErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
		}
	}

	/**
	 * Google Refresh Token으로 새 Access Token 발급
	 *
	 * @param refreshToken Google Refresh Token (복호화된 평문)
	 * @return Google Token Response (accessToken, expiresIn)
	 */
	public GoogleTokenResponse refreshGoogleToken(String refreshToken) {
		try {
			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("refresh_token", refreshToken);
			params.add("client_id", googleOAuthProperties.getClientId());
			params.add("client_secret", googleOAuthProperties.getClientSecret());
			params.add("grant_type", "refresh_token");

			GoogleTokenResponse response = restClient.post()
				.uri(GOOGLE_TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(params)
				.retrieve()
				.body(GoogleTokenResponse.class);

			if (response == null || response.accessToken() == null) {
				log.error("Google 토큰 갱신 실패: 응답이 null이거나 accessToken이 없습니다");
				throw new CustomException(ErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
			}

			log.info("Google 토큰 갱신 성공");
			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Google 토큰 갱신 중 오류 발생", e);
			throw new CustomException(ErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
		}
	}

	/**
	 * Google ID Token을 검증하고 사용자 정보를 추출
	 *
	 * @param idToken Google ID Token
	 * @return Google ID Token Payload (sub, email, emailVerified)
	 */
	public GoogleIdTokenPayload verifyIdToken(String idToken) {
		try {
			GoogleIdTokenPayload payload = restClient.get()
				.uri(GOOGLE_TOKEN_INFO_URL + "?id_token=" + idToken)
				.retrieve()
				.body(GoogleIdTokenPayload.class);

			if (payload == null || payload.sub() == null || payload.email() == null) {
				log.error("Google ID Token 검증 실패: 응답이 null이거나 필수 정보가 없습니다");
				throw new CustomException(ErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
			}

			if (!payload.emailVerified()) {
				log.warn("Google 이메일이 검증되지 않았습니다: {}", maskEmail(payload.email()));
				throw new CustomException(ErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
			}

			log.info("Google ID Token 검증 성공: sub={}", maskSub(payload.sub()));
			return payload;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Google ID Token 검증 중 오류 발생", e);
			throw new CustomException(ErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
		}
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
