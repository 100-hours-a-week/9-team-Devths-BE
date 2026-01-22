package com.ktb3.devths.chatbot.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.ktb3.devths.chatbot.domain.entity.AiChatMessage;

public record AiChatMessageResponse(
	Long roomId,
	Long messageId,
	Long interviewId,
	String role,
	String content,
	String type,
	Map<String, Object> metadata,
	LocalDateTime createdAt
) {
	public static AiChatMessageResponse from(AiChatMessage message) {
		return new AiChatMessageResponse(
			message.getRoom().getId(),
			message.getId(),
			message.getInterview() != null ? message.getInterview().getId() : null,
			message.getRole().name(),
			message.getContent(),
			message.getType().name(),
			message.getMetadata(),
			message.getCreatedAt()
		);
	}
}
