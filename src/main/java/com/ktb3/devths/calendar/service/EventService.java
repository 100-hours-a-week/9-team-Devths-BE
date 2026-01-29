package com.ktb3.devths.calendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;
import com.ktb3.devths.calendar.domain.constant.NotificationUnit;
import com.ktb3.devths.calendar.dto.internal.GoogleEventMapping;
import com.ktb3.devths.calendar.dto.request.EventCreateRequest;
import com.ktb3.devths.calendar.dto.response.EventCreateResponse;
import com.ktb3.devths.calendar.dto.response.EventDetailResponse;
import com.ktb3.devths.calendar.dto.response.EventListResponse;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

	private final GoogleCalendarService googleCalendarService;
	private final UserRepository userRepository;

	/**
	 * Google Calendar 일정 추가
	 *
	 * @param userId 사용자 ID
	 * @param request 일정 추가 요청
	 * @return 일정 추가 응답 (Google Calendar Event ID)
	 */
	@Transactional
	public EventCreateResponse createEvent(Long userId, EventCreateRequest request) {
		// 1. 사용자 조회 및 검증
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (user.isWithdraw()) {
			throw new CustomException(ErrorCode.WITHDRAWN_USER);
		}

		// 2. Google Calendar 매핑 객체 생성
		GoogleEventMapping mapping = GoogleEventMapping.builder()
			.summary(request.title())
			.company(request.company())
			.description(request.description())
			.startTime(request.startTime())
			.endTime(request.endTime())
			.stage(request.stage())
			.tags(request.tags())
			.notificationMinutes(convertToMinutes(request.notificationTime(), request.notificationUnit()))
			.build();

		// 3. Google Calendar API 호출
		String eventId = googleCalendarService.createEvent(userId, mapping);

		log.info("일정 추가 완료: userId={}, eventId={}", userId, eventId);
		return EventCreateResponse.of(eventId);
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
		LocalDate startDate,
		LocalDate endDate,
		InterviewStage stage,
		String tag
	) {
		// 1. 사용자 조회 및 검증
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (user.isWithdraw()) {
			throw new CustomException(ErrorCode.WITHDRAWN_USER);
		}

		// 2. 날짜 범위를 LocalDateTime으로 변환 (Asia/Seoul 기준)
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

		// 3. Google Calendar API 호출
		return googleCalendarService.listEvents(userId, startDateTime, endDateTime, stage, tag);
	}

	/**
	 * Google Calendar 일정 상세 조회
	 *
	 * @param userId 사용자 ID
	 * @param eventId Google Calendar Event ID
	 * @return 일정 상세 정보
	 */
	@Transactional(readOnly = true)
	public EventDetailResponse getEvent(Long userId, String eventId) {
		// 1. 사용자 조회 및 검증
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (user.isWithdraw()) {
			throw new CustomException(ErrorCode.WITHDRAWN_USER);
		}

		// 2. Google Calendar API 호출
		return googleCalendarService.getEvent(userId, eventId);
	}

	/**
	 * 알림 시간을 분 단위로 변환
	 */
	private int convertToMinutes(int notificationTime, NotificationUnit unit) {
		return switch (unit) {
			case MINUTE -> notificationTime;
			case HOUR -> notificationTime * 60;
			case DAY -> notificationTime * 60 * 24;
		};
	}
}
