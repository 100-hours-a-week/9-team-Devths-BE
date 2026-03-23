package com.ktb3.devths.notification.dto.request;

import com.ktb3.devths.notification.domain.constant.DeviceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FcmTokenRegisterRequest(
	@NotBlank(message = "FCM 토큰은 필수입니다")
	String token,

	@NotNull(message = "디바이스 타입은 필수입니다")
	DeviceType deviceType
) {
}
