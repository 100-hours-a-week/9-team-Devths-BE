package com.ktb3.devths.todo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TodoUpdateRequest(
	@JsonProperty("title")
	@NotBlank(message = "제목은 필수입니다")
	String title,

	@JsonProperty("dueDate")
	@NotNull(message = "마감일은 필수입니다")
	@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "마감일 형식은 yyyy-MM-dd 이어야 합니다")
	String dueDate
) {
}
