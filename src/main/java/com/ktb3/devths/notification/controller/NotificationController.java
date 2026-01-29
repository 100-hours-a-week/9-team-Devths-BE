package com.ktb3.devths.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;
import com.ktb3.devths.notification.dto.response.NotificationListResponse;
import com.ktb3.devths.notification.dto.response.UnreadCountResponse;
import com.ktb3.devths.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<ApiResponse<NotificationListResponse>> getNotificationList(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) Long lastId
	) {
		NotificationListResponse response = notificationService.getNotificationList(
			userPrincipal.getUserId(),
			size,
			lastId
		);

		return ResponseEntity.ok(
			ApiResponse.success("알림 목록을 성공적으로 조회하였습니다.", response)
		);
	}

	@GetMapping("/unread")
	public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		UnreadCountResponse response = notificationService.getUnreadCount(
			userPrincipal.getUserId()
		);

		return ResponseEntity.ok(
			ApiResponse.success("안 읽은 알림 개수를 성공적으로 조회하였습니다.", response)
		);
	}
}
