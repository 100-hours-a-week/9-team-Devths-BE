package com.ktb3.devths.ai.chatbot.dto.response;

import java.time.LocalDateTime;

import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;

public record CurrentInterviewResponse(
	Long interviewId,
	String interviewType,
	int currentQuestionCount,
	LocalDateTime createdAt
) {
	public static CurrentInterviewResponse from(AiChatInterview interview) {
		return new CurrentInterviewResponse(
			interview.getId(),
			interview.getInterviewType().name(),
			interview.getCurrentQuestionCount(),
			interview.getCreatedAt()
		);
	}
}
