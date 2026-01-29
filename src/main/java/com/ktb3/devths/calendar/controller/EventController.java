package com.ktb3.devths.calendar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.calendar.dto.request.EventCreateRequest;
import com.ktb3.devths.calendar.dto.response.EventCreateResponse;
import com.ktb3.devths.calendar.service.EventService;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

	private final EventService eventService;

	/**
	 * Google Calendar 일정 추가
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param request 일정 추가 요청
	 * @return 일정 추가 응답
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<EventCreateResponse>> createEvent(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@Valid @RequestBody EventCreateRequest request
	) {
		EventCreateResponse response = eventService.createEvent(userPrincipal.getUserId(), request);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success("일정이 성공적으로 추가되었습니다.", response));
	}
}
