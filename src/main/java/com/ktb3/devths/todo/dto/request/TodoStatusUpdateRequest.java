package com.ktb3.devths.todo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record TodoStatusUpdateRequest(
	@JsonProperty("isCompleted")
	@NotNull(message = "완료 상태는 필수입니다")
	Boolean isCompleted
) {
}
