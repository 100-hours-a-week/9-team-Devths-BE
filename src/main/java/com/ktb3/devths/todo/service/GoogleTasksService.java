package com.ktb3.devths.todo.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.ktb3.devths.auth.dto.internal.GoogleTokenResponse;
import com.ktb3.devths.auth.service.GoogleOAuthService;
import com.ktb3.devths.auth.service.TokenEncryptionService;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.user.domain.entity.SocialAccount;
import com.ktb3.devths.user.repository.SocialAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTasksService {

	private static final String APPLICATION_NAME = "Devths";

	private final SocialAccountRepository socialAccountRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthService googleOAuthService;

	/**
	 * Google Tasks 목록 조회
	 *
	 * @param userId 사용자 ID
	 * @return Google Tasks 목록
	 */
	@Transactional(readOnly = true)
	public List<Task> listTasks(Long userId) {
		try {
			// 1. SocialAccount 조회
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			// 2. 토큰 만료 체크 및 갱신
			refreshAccessTokenIfExpired(socialAccount);

			// 3. Google Tasks 클라이언트 생성
			Tasks tasksService = buildTasksClient(socialAccount);

			// 4. Google Tasks API 호출
			List<Task> tasks = tasksService.tasks()
				.list("@default")           // 기본 task list
				.setShowCompleted(true)     // 완료 작업 포함
				.setShowHidden(false)       // 삭제 작업 제외
				.execute()
				.getItems();

			log.info("Google Tasks 조회 성공: userId={}, count={}", userId, tasks != null ? tasks.size() : 0);
			return tasks != null ? tasks : List.of();

		} catch (CustomException e) {
			throw e;
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 403) {
				log.error("Google Tasks 접근 권한 없음: userId={}", userId, e);
				throw new CustomException(ErrorCode.GOOGLE_TASKS_ACCESS_DENIED);
			}
			log.error("Google Tasks 조회 실패: userId={}, statusCode={}", userId, e.getStatusCode(), e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		} catch (Exception e) {
			log.error("Google Tasks 조회 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		}
	}

	/**
	 * Google Tasks 클라이언트 생성
	 */
	private Tasks buildTasksClient(SocialAccount socialAccount) throws GeneralSecurityException, IOException {
		String decryptedAccessToken = tokenEncryptionService.decrypt(socialAccount.getAccessToken());

		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

		GoogleCredentials credentials = GoogleCredentials.create(
			new AccessToken(decryptedAccessToken, null)
		);

		return new Tasks.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
			.setApplicationName(APPLICATION_NAME)
			.build();
	}

	/**
	 * 토큰 만료 체크 및 갱신
	 */
	@Transactional
	public void refreshAccessTokenIfExpired(SocialAccount socialAccount) {
		if (socialAccount.getExpiresAt().isBefore(LocalDateTime.now())) {
			log.info("Google Access Token 만료 감지, 토큰 갱신 시작: userId={}", socialAccount.getUser().getId());

			String decryptedRefreshToken = tokenEncryptionService.decrypt(socialAccount.getRefreshToken());

			GoogleTokenResponse tokenResponse = googleOAuthService.refreshGoogleToken(decryptedRefreshToken);

			String encryptedAccessToken = tokenEncryptionService.encrypt(tokenResponse.accessToken());
			String encryptedRefreshToken = tokenResponse.refreshToken() != null
				? tokenEncryptionService.encrypt(tokenResponse.refreshToken())
				: socialAccount.getRefreshToken();

			LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.expiresIn());

			socialAccount.updateTokens(encryptedAccessToken, encryptedRefreshToken, expiresAt);

			log.info("Google Access Token 갱신 완료: userId={}", socialAccount.getUser().getId());
		}
	}
}
