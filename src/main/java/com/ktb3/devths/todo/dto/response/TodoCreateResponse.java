package com.ktb3.devths.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TodoCreateResponse(
	@JsonProperty("todoId")
	String todoId
) {
}
