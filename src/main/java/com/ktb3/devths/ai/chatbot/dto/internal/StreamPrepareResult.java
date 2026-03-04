package com.ktb3.devths.ai.chatbot.dto.internal;

import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiChatRequest;
import com.ktb3.devths.ai.constant.AiModel;

public record StreamPrepareResult(
	Long roomId,
	Long interviewId,
	MessageType messageType,
	AiModel model,
	FastApiChatRequest request
) {
}
