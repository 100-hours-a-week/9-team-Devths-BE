package com.ktb3.devths.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TodoStatusUpdateResponse(
	@JsonProperty("todoId")
	String todoId,

	@JsonProperty("isCompleted")
	boolean isCompleted
) {
}
