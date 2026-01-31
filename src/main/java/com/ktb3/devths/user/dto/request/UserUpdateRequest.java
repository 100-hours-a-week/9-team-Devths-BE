package com.ktb3.devths.user.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
	@NotBlank(message = "닉네임은 필수입니다")
	String nickname,
	List<String> interests
) {
}
