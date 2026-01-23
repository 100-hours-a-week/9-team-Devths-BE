package com.ktb3.devths.chatbot.dto.response;

import java.util.List;

import com.ktb3.devths.chatbot.domain.entity.AiChatMessage;

public record AiChatMessageListResponse(
	List<AiChatMessageResponse> messages,
	Long lastId,
	boolean hasNext
) {
	public static AiChatMessageListResponse of(List<AiChatMessage> chatMessages, int requestedSize) {
		boolean hasNext = chatMessages.size() > requestedSize;

		List<AiChatMessage> actualMessages = hasNext
			? chatMessages.subList(0, requestedSize)
			: chatMessages;

		List<AiChatMessageResponse> messages = actualMessages.stream()
			.map(AiChatMessageResponse::from)
			.toList();

		Long lastId = messages.isEmpty() ? null : messages.getLast().messageId();

		return new AiChatMessageListResponse(messages, lastId, hasNext);
	}
}
