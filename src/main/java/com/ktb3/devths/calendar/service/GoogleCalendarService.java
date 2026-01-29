package com.ktb3.devths.calendar.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.ktb3.devths.auth.dto.internal.GoogleTokenResponse;
import com.ktb3.devths.auth.service.GoogleOAuthService;
import com.ktb3.devths.auth.service.TokenEncryptionService;
import com.ktb3.devths.calendar.domain.constant.InterviewStage;
import com.ktb3.devths.calendar.dto.internal.GoogleEventMapping;
import com.ktb3.devths.calendar.dto.response.EventListResponse;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.user.domain.entity.SocialAccount;
import com.ktb3.devths.user.repository.SocialAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

	private static final String TIMEZONE = "Asia/Seoul";
	private static final String APPLICATION_NAME = "Devths";

	private final SocialAccountRepository socialAccountRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthService googleOAuthService;
	private final ObjectMapper objectMapper;

	/**
	 * Google Calendar에 일정 추가
	 *
	 * @param userId 사용자 ID
	 * @param mapping Google Calendar Event 매핑 데이터
	 * @return Google Calendar Event ID
	 */
	@Transactional
	public String createEvent(Long userId, GoogleEventMapping mapping) {
		try {
			// 1. SocialAccount 조회
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			// 2. 토큰 만료 체크 및 갱신
			refreshAccessTokenIfExpired(socialAccount);

			// 3. Google Calendar 클라이언트 생성
			Calendar calendarService = buildCalendarClient(socialAccount);

			// 4. Event 객체 생성
			Event event = createGoogleEvent(mapping);

			// 5. Google Calendar API 호출
			Event createdEvent = calendarService.events()
				.insert("primary", event)
				.execute();

			log.info("Google Calendar 일정 추가 성공: userId={}, eventId={}", userId, createdEvent.getId());
			return createdEvent.getId();
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Google Calendar 일정 추가 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_CALENDAR_CREATE_FAILED);
		}
	}

	/**
	 * Google Calendar 일정 목록 조회
	 *
	 * @param userId 사용자 ID
	 * @param startDate 조회 시작 날짜
	 * @param endDate 조회 종료 날짜
	 * @param stage 필터: 전형 단계 (선택)
	 * @param tag 필터: 태그 (선택)
	 * @return 일정 목록
	 */
	@Transactional(readOnly = true)
	public List<EventListResponse> listEvents(
		Long userId,
		LocalDateTime startDate,
		LocalDateTime endDate,
		InterviewStage stage,
		String tag
	) {
		try {
			// 1. SocialAccount 조회
			SocialAccount socialAccount = socialAccountRepository.findByUser_IdAndProvider(userId, "GOOGLE")
				.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

			// 2. 토큰 만료 체크 및 갱신
			refreshAccessTokenIfExpired(socialAccount);

			// 3. Google Calendar 클라이언트 생성
			Calendar calendarService = buildCalendarClient(socialAccount);

			// 4. Google Calendar API 호출
			DateTime timeMin = convertToDateTime(startDate);
			DateTime timeMax = convertToDateTime(endDate);

			// privateExtendedProperty 리스트 구성
			List<String> privateExtendedProperties = new ArrayList<>();
			privateExtendedProperties.add("source=devths");
			if (stage != null) {
				privateExtendedProperties.add("stage=" + stage.name());
			}

			com.google.api.services.calendar.Calendar.Events.List request = calendarService.events()
				.list("primary")
				.setTimeMin(timeMin)
				.setTimeMax(timeMax)
				.setSingleEvents(true)
				.setOrderBy("startTime")
				.setPrivateExtendedProperty(privateExtendedProperties);

			List<Event> events = request.execute().getItems();

			// 5. 응답 변환 및 필터링 (tag는 백엔드에서 필터링)
			return events.stream()
				.filter(event -> matchesTagFilter(event, tag))
				.map(this::convertToEventListResponse)
				.toList();

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Google Calendar 일정 목록 조회 실패: userId={}", userId, e);
			throw new CustomException(ErrorCode.GOOGLE_CALENDAR_CREATE_FAILED);
		}
	}

	/**
	 * tag 필터 매칭 (백엔드에서 처리)
	 */
	private boolean matchesTagFilter(Event event, String tag) {
		if (tag == null) {
			return true;
		}
		String tagsJson = (String)event.getExtendedProperties().getPrivate().get("tags");
		try {
			List<String> tags = objectMapper.readValue(tagsJson, List.class);
			return tags.contains(tag);
		} catch (Exception e) {
			log.warn("태그 파싱 실패: eventId={}", event.getId());
			return false;
		}
	}

	/**
	 * Google Event를 EventListResponse로 변환
	 */
	private EventListResponse convertToEventListResponse(Event event) {
		try {
			LocalDateTime startTime = convertToLocalDateTime(event.getStart().getDateTime());
			LocalDateTime endTime = convertToLocalDateTime(event.getEnd().getDateTime());

			String stageStr = (String)event.getExtendedProperties().getPrivate().get("stage");
			InterviewStage stage = InterviewStage.valueOf(stageStr);

			String tagsJson = (String)event.getExtendedProperties().getPrivate().get("tags");
			List<String> tags = objectMapper.readValue(tagsJson, List.class);

			return EventListResponse.of(
				event.getId(),
				event.getSummary(),
				startTime,
				endTime,
				stage,
				tags
			);
		} catch (Exception e) {
			log.error("Event 변환 실패: eventId={}", event.getId(), e);
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Google Calendar DateTime을 LocalDateTime으로 변환
	 */
	private LocalDateTime convertToLocalDateTime(DateTime dateTime) {
		return LocalDateTime.ofInstant(
			java.time.Instant.ofEpochMilli(dateTime.getValue()),
			ZoneId.of(TIMEZONE)
		);
	}

	/**
	 * Google Calendar 클라이언트 생성
	 */
	private Calendar buildCalendarClient(SocialAccount socialAccount) throws GeneralSecurityException, IOException {
		String decryptedAccessToken = tokenEncryptionService.decrypt(socialAccount.getAccessToken());

		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

		GoogleCredentials credentials = GoogleCredentials.create(
			new AccessToken(decryptedAccessToken, null)
		);

		return new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
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

	/**
	 * Google Calendar Event 객체 생성
	 */
	private Event createGoogleEvent(GoogleEventMapping mapping) throws JsonProcessingException {
		Event event = new Event();

		// 기본 필드
		event.setSummary(mapping.summary());
		if (mapping.description() != null) {
			event.setDescription(mapping.description());
		}

		// 시작/종료 시간 (Asia/Seoul 타임존)
		EventDateTime start = new EventDateTime()
			.setDateTime(convertToDateTime(mapping.startTime()))
			.setTimeZone(TIMEZONE);
		EventDateTime end = new EventDateTime()
			.setDateTime(convertToDateTime(mapping.endTime()))
			.setTimeZone(TIMEZONE);
		event.setStart(start);
		event.setEnd(end);

		// ExtendedProperties 설정
		Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
		extendedProperties.setPrivate(java.util.Map.of(
			"stage", mapping.stage().name(),
			"company", mapping.company(),
			"tags", mapping.tags() != null ? objectMapper.writeValueAsString(mapping.tags()) : "[]",
			"source", "devths"
		));
		event.setExtendedProperties(extendedProperties);

		// 알림 설정
		EventReminder reminder = new EventReminder()
			.setMethod("popup")
			.setMinutes(mapping.notificationMinutes());
		Event.Reminders reminders = new Event.Reminders()
			.setUseDefault(false)
			.setOverrides(List.of(reminder));
		event.setReminders(reminders);

		return event;
	}

	/**
	 * LocalDateTime을 Google Calendar DateTime으로 변환
	 */
	private DateTime convertToDateTime(LocalDateTime localDateTime) {
		long millis = localDateTime.atZone(ZoneId.of(TIMEZONE)).toInstant().toEpochMilli();
		return new DateTime(millis);
	}
}
