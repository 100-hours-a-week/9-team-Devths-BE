package com.ktb3.devths.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TodoResponse(
	@JsonProperty("todoId")
	String todoId,

	@JsonProperty("title")
	String title,

	@JsonProperty("isCompleted")
	boolean isCompleted,

	@JsonProperty("dueDate")
	String dueDate
) {
}
