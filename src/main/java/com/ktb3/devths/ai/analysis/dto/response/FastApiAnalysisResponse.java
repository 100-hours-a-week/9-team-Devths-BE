package com.ktb3.devths.ai.analysis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiAnalysisResponse(
	@JsonProperty("task_id")
	Long taskId,

	String status
) {
}
