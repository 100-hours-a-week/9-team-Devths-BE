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
	 * Google Tasks 추가
	 *
	 * @param userId 사용자 ID
	 * @param title 할 일 제목
	 * @param dueDate 마감일 (yyyy-MM-dd)
	 * @return Google Tasks ID
	 */
	@Transactional
	public String createTask(Long userId, String title, String dueDate) {
		try {
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			refreshAccessTokenIfExpired(socialAccount);

			Tasks tasksService = buildTasksClient(socialAccount);

			Task task = new Task();
			task.setTitle(title);
			task.setDue(convertToRfc3339(dueDate));
			task.setStatus("needsAction");

			Task createdTask = tasksService.tasks()
				.insert("@default", task)
				.execute();

			log.info("Google Tasks 추가 성공: userId={}, taskId={}", userId, createdTask.getId());
			return createdTask.getId();

		} catch (CustomException e) {
			throw e;
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 403) {
				log.error("Google Tasks 접근 권한 없음: userId={}", userId, e);
				throw new CustomException(ErrorCode.GOOGLE_TASKS_ACCESS_DENIED);
			}
			log.error("Google Tasks 추가 실패: userId={}, statusCode={}", userId, e.getStatusCode(), e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		} catch (Exception e) {
			log.error("Google Tasks 추가 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		}
	}

	/**
	 * Google Tasks 수정
	 *
	 * @param userId 사용자 ID
	 * @param todoId 할 일 ID
	 * @param title 할 일 제목
	 * @param dueDate 마감일 (yyyy-MM-dd)
	 * @return Google Tasks ID
	 */
	@Transactional
	public String updateTask(Long userId, String todoId, String title, String dueDate) {
		try {
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			refreshAccessTokenIfExpired(socialAccount);

			Tasks tasksService = buildTasksClient(socialAccount);

			// 기존 Task 조회 (존재 여부 확인)
			Task existingTask = tasksService.tasks()
				.get("@default", todoId)
				.execute();

			// Task 업데이트
			existingTask.setTitle(title);
			existingTask.setDue(convertToRfc3339(dueDate));

			Task updatedTask = tasksService.tasks()
				.update("@default", todoId, existingTask)
				.execute();

			log.info("Google Tasks 수정 성공: userId={}, taskId={}", userId, updatedTask.getId());
			return updatedTask.getId();

		} catch (CustomException e) {
			throw e;
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 404) {
				log.error("Google Tasks 찾을 수 없음: userId={}, todoId={}", userId, todoId, e);
				throw new CustomException(ErrorCode.TODO_NOT_FOUND);
			}
			if (e.getStatusCode() == 403) {
				log.error("Google Tasks 접근 권한 없음: userId={}", userId, e);
				throw new CustomException(ErrorCode.GOOGLE_TASKS_ACCESS_DENIED);
			}
			log.error("Google Tasks 수정 실패: userId={}, statusCode={}", userId, e.getStatusCode(), e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		} catch (Exception e) {
			log.error("Google Tasks 수정 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		}
	}

	/**
	 * Google Tasks 완료 상태 변경
	 *
	 * @param userId 사용자 ID
	 * @param todoId 할 일 ID
	 * @param isCompleted 완료 상태
	 * @return Task 객체 (id, status 포함)
	 */
	@Transactional
	public Task updateTaskStatus(Long userId, String todoId, boolean isCompleted) {
		try {
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			refreshAccessTokenIfExpired(socialAccount);

			Tasks tasksService = buildTasksClient(socialAccount);

			// 기존 Task 조회
			Task existingTask = tasksService.tasks()
				.get("@default", todoId)
				.execute();

			// 상태 변경
			String newStatus = isCompleted ? "completed" : "needsAction";
			existingTask.setStatus(newStatus);

			Task updatedTask = tasksService.tasks()
				.update("@default", todoId, existingTask)
				.execute();

			log.info("Google Tasks 상태 변경 성공: userId={}, taskId={}, status={}", userId, updatedTask.getId(), newStatus);
			return updatedTask;

		} catch (CustomException e) {
			throw e;
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 404) {
				log.error("Google Tasks 찾을 수 없음: userId={}, todoId={}", userId, todoId, e);
				throw new CustomException(ErrorCode.TODO_NOT_FOUND);
			}
			if (e.getStatusCode() == 403) {
				log.error("Google Tasks 접근 권한 없음: userId={}", userId, e);
				throw new CustomException(ErrorCode.GOOGLE_TASKS_ACCESS_DENIED);
			}
			log.error("Google Tasks 상태 변경 실패: userId={}, statusCode={}", userId, e.getStatusCode(), e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		} catch (Exception e) {
			log.error("Google Tasks 상태 변경 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		}
	}

	/**
	 * Google Tasks 삭제
	 *
	 * @param userId 사용자 ID
	 * @param todoId 할 일 ID
	 */
	@Transactional
	public void deleteTask(Long userId, String todoId) {
		try {
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			refreshAccessTokenIfExpired(socialAccount);

			Tasks tasksService = buildTasksClient(socialAccount);

			tasksService.tasks()
				.delete("@default", todoId)
				.execute();

			log.info("Google Tasks 삭제 성공: userId={}, taskId={}", userId, todoId);

		} catch (CustomException e) {
			throw e;
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 404) {
				log.error("Google Tasks 찾을 수 없음: userId={}, todoId={}", userId, todoId, e);
				throw new CustomException(ErrorCode.TODO_NOT_FOUND);
			}
			if (e.getStatusCode() == 403) {
				log.error("Google Tasks 접근 권한 없음: userId={}", userId, e);
				throw new CustomException(ErrorCode.GOOGLE_TASKS_ACCESS_DENIED);
			}
			log.error("Google Tasks 삭제 실패: userId={}, statusCode={}", userId, e.getStatusCode(), e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		} catch (Exception e) {
			log.error("Google Tasks 삭제 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_TASKS_UNAVAILABLE);
		}
	}

	private String convertToRfc3339(String dueDate) {
		java.time.LocalDate localDate = java.time.LocalDate.parse(dueDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		java.time.Instant instant = localDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant();
		return instant.toString();
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
