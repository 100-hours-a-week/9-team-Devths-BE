package com.ktb3.devths.ai.chatbot.dto.request;

import com.ktb3.devths.ai.chatbot.domain.constant.InterviewType;
import com.ktb3.devths.ai.constant.AiModel;

import jakarta.validation.constraints.NotNull;

public record InterviewStartRequest(
	@NotNull(message = "면접 유형은 필수입니다")
	InterviewType interviewType,

	@NotNull(message = "AI 모델은 필수입니다")
	AiModel model
) {
}
