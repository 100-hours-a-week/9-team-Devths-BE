package com.ktb3.devths.notification.dto.response;

import java.time.LocalDateTime;

import com.ktb3.devths.notification.domain.entity.FcmToken;

public record FcmTokenRegisterResponse(
	Long tokenId,
	LocalDateTime createdAt
) {
	public static FcmTokenRegisterResponse from(FcmToken fcmToken) {
		return new FcmTokenRegisterResponse(fcmToken.getId(), fcmToken.getCreatedAt());
	}
}
