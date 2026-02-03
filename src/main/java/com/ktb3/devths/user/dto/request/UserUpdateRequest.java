package com.ktb3.devths.user.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
	@NotBlank(message = "닉네임은 필수입니다")
	@Size(min = 2, max = 10)
	String nickname,
	List<String> interests
) {
}
