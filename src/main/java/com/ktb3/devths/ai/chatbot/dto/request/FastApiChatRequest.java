package com.ktb3.devths.ai.chatbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiChatRequest(
	String model,

	@JsonProperty("room_id")
	Long roomId,

	@JsonProperty("user_id")
	Long userId,

	String message,

	@JsonProperty("interview_id")
	Long interviewId,

	FastApiChatContext context
) {
}
