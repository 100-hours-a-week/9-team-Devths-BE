package com.ktb3.devths.ai.chatbot.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiInterviewEvaluationRequest(
	@JsonProperty("interview_id")
	Long interviewId,

	@JsonProperty("interview_type")
	String interviewType,

	@JsonProperty("room_id")
	Long roomId,

	@JsonProperty("user_id")
	Long userId,

	List<FastApiInterviewMessage> messages
) {
	public record FastApiInterviewMessage(
		String role,
		String content
	) {
	}
}
