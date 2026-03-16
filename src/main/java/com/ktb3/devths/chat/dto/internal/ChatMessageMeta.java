package com.ktb3.devths.chat.dto.internal;

public record ChatMessageMeta(
	String eventType,
	Long roomId,
	Long messageId,
	String chatSessionId
) {
}
