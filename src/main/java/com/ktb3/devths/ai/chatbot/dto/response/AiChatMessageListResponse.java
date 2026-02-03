package com.ktb3.devths.ai.chatbot.dto.response;

import java.util.Collections;
import java.util.List;

import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;

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

		// DESC로 조회했으므로 reverse하여 ASC 순서로 변환
		List<AiChatMessage> ordered = new java.util.ArrayList<>(actualMessages);
		Collections.reverse(ordered);

		List<AiChatMessageResponse> messages = ordered.stream()
			.map(AiChatMessageResponse::from)
			.toList();

		// 스크롤 위로 올릴 때 사용할 커서 (가장 오래된 메시지 ID)
		Long lastId = messages.isEmpty() ? null : messages.getFirst().messageId();

		return new AiChatMessageListResponse(messages, lastId, hasNext);
	}
}
