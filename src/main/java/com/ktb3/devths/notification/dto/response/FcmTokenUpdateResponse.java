package com.ktb3.devths.notification.dto.response;

import java.time.LocalDateTime;

import com.ktb3.devths.notification.domain.entity.FcmToken;

public record FcmTokenUpdateResponse(
	Boolean isActive,
	LocalDateTime updatedAt
) {
	public static FcmTokenUpdateResponse from(FcmToken fcmToken) {
		return new FcmTokenUpdateResponse(fcmToken.getIsActive(), fcmToken.getUpdatedAt());
	}
}
