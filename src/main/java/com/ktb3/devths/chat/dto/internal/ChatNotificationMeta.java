package com.ktb3.devths.chat.dto.internal;

public record ChatNotificationMeta(
	String eventType,
	Long userId,
	Long roomId,
	String chatSessionId
) {
}
