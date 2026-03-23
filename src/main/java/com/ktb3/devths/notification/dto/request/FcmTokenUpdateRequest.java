package com.ktb3.devths.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record FcmTokenUpdateRequest(
	@NotNull(message = "알림 활성화 여부는 필수입니다")
	Boolean isActive
) {
}
