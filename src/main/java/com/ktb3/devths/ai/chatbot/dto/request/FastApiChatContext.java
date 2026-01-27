package com.ktb3.devths.ai.chatbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

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
			"NORMAL",
			null,
			null,
			null,
			null
		);
	}
}
