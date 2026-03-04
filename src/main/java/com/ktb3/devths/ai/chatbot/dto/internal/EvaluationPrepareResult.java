package com.ktb3.devths.ai.chatbot.dto.internal;

import com.ktb3.devths.ai.chatbot.domain.constant.InterviewType;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiInterviewEvaluationRequest;

public record EvaluationPrepareResult(
	Long interviewId,
	Long roomId,
	InterviewType interviewType,
	FastApiInterviewEvaluationRequest request
) {
}
