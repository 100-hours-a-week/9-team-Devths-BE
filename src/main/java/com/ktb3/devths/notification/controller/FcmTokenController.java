package com.ktb3.devths.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;
import com.ktb3.devths.notification.dto.request.FcmTokenRegisterRequest;
import com.ktb3.devths.notification.dto.response.FcmTokenRegisterResponse;
import com.ktb3.devths.notification.service.FcmTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications/tokens")
@RequiredArgsConstructor
public class FcmTokenController {

	private final FcmTokenService fcmTokenService;

	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201")
	@PostMapping("/{deviceId}")
	public ResponseEntity<ApiResponse<FcmTokenRegisterResponse>> registerToken(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable String deviceId,
		@Valid @RequestBody FcmTokenRegisterRequest request
	) {
		FcmTokenRegisterResponse response = fcmTokenService.registerToken(
			userPrincipal.getUserId(),
			deviceId,
			request
		);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("푸시 토큰이 성공적으로 등록되었습니다.", response));
	}
}
