package com.ktb3.devths.analysis.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiAnalysisRequest(
	String model,

	@JsonProperty("room_id")
	Long roomId,

	@JsonProperty("user_id")
	Long userId,

	FastApiDocumentInfo resume,

	@JsonProperty("job_posting")
	FastApiDocumentInfo jobPosting
) {
	public record FastApiDocumentInfo(
		@JsonProperty("file_id")
		Long fileId,

		@JsonProperty("s3_key")
		String s3Key,

		@JsonProperty("file_type")
		String fileType,

		String text
	) {
	}
}
