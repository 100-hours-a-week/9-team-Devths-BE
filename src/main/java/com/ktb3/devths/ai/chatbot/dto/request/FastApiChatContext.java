package com.ktb3.devths.ai.chatbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;

public record FastApiChatContext(
	String mode,

	@JsonProperty("resume_ocr")
	String resumeOcr,

	@JsonProperty("job_posting_ocr")
	String jobPostingOcr,

	@JsonProperty("interview_type")
	String interviewType,

	@JsonProperty("question_count")
	Integer questionCount
) {
	public static FastApiChatContext createNormalMode() {
		return new FastApiChatContext(
			MessageType.NORMAL.name().toLowerCase(),
			null,
			null,
			null,
			null
		);
	}
}
