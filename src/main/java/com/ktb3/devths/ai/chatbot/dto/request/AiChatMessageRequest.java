package com.ktb3.devths.ai.chatbot.dto.request;

import com.ktb3.devths.ai.constant.AiModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiChatMessageRequest(
	@NotBlank(message = "메시지 내용은 필수입니다")
	String content,

	@NotNull(message = "AI 모델은 필수입니다")
	AiModel model
) {
}
