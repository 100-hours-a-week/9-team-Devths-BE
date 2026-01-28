package com.ktb3.devths.ai.analysis.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiTaskStatusResponse(
	@JsonProperty("task_id")
	Long taskId,

	String status,

	Map<String, Object> result
) {
}
