package com.ktb3.devths.ai.chatbot.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiInterviewEvaluationRequest(
	@JsonProperty("interview_id")
	Long interviewId,

	@JsonProperty("interview_type")
	String interviewType,

	List<FastApiInterviewMessage> messages
) {
	public record FastApiInterviewMessage(
		String role,
		String content
	) {
	}
}
