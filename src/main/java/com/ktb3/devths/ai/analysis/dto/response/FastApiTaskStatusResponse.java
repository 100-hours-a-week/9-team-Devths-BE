package com.ktb3.devths.ai.analysis.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiTaskStatusResponse(
	@JsonProperty("task_id")
	String taskId,

	String status,

	Map<String, Object> result
) {
}
