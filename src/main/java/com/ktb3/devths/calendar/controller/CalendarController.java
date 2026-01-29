package com.ktb3.devths.calendar.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;
import com.ktb3.devths.calendar.dto.request.GoogleEventCreateRequest;
import com.ktb3.devths.calendar.dto.request.GoogleEventUpdateRequest;
import com.ktb3.devths.calendar.dto.response.GoogleEventCreateResponse;
import com.ktb3.devths.calendar.dto.response.GoogleEventDetailResponse;
import com.ktb3.devths.calendar.dto.response.GoogleEventListResponse;
import com.ktb3.devths.calendar.service.CalendarService;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

	private final CalendarService calendarService;

	/**
	 * Google Calendar 일정 추가
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param request 일정 추가 요청
	 * @return 일정 추가 응답
	 */
	@PostMapping
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201")
	public ResponseEntity<ApiResponse<GoogleEventCreateResponse>> createEvent(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@Valid @RequestBody GoogleEventCreateRequest request
	) {
		GoogleEventCreateResponse response = calendarService.createEvent(userPrincipal.getUserId(), request);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success("일정이 성공적으로 추가되었습니다.", response));
	}

	/**
	 * Google Calendar 일정 목록 조회
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param startDate 조회 시작 날짜 (필수, yyyy-MM-dd)
	 * @param endDate 조회 종료 날짜 (필수, yyyy-MM-dd)
	 * @param stage 전형 단계 필터 (선택)
	 * @param tag 태그 필터 (선택)
	 * @return 일정 목록
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<GoogleEventListResponse>>> listEvents(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
		@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
		@RequestParam(required = false) InterviewStage stage,
		@RequestParam(required = false) String tag
	) {
		List<GoogleEventListResponse> response = calendarService.listEvents(
			userPrincipal.getUserId(),
			startDate,
			endDate,
			stage,
			tag
		);

		return ResponseEntity
			.ok(ApiResponse.success("일정 목록을 성공적으로 조회하였습니다.", response));
	}

	/**
	 * Google Calendar 일정 상세 조회
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param eventId Google Calendar Event ID
	 * @return 일정 상세 정보
	 */
	@GetMapping("/{eventId}")
	public ResponseEntity<ApiResponse<GoogleEventDetailResponse>> getEvent(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable String eventId
	) {
		GoogleEventDetailResponse response = calendarService.getEvent(userPrincipal.getUserId(), eventId);

		return ResponseEntity
			.ok(ApiResponse.success("일정 상세 정보를 성공적으로 조회하였습니다.", response));
	}

	/**
	 * Google Calendar 일정 수정
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param eventId Google Calendar Event ID
	 * @param request 일정 수정 요청
	 * @return 일정 수정 응답
	 */
	@PutMapping("/{eventId}")
	public ResponseEntity<ApiResponse<GoogleEventCreateResponse>> updateEvent(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable String eventId,
		@Valid @RequestBody GoogleEventUpdateRequest request
	) {
		GoogleEventCreateResponse response = calendarService.updateEvent(
			userPrincipal.getUserId(),
			eventId,
			request
		);

		return ResponseEntity
			.ok(ApiResponse.success("일정이 성공적으로 수정되었습니다.", response));
	}

	/**
	 * Google Calendar 일정 삭제
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param eventId Google Calendar Event ID
	 * @return 204 No Content
	 */
	@DeleteMapping("/{eventId}")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204")
	public ResponseEntity<Void> deleteEvent(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable String eventId
	) {
		calendarService.deleteEvent(userPrincipal.getUserId(), eventId);

		return ResponseEntity.noContent().build();
	}
}
