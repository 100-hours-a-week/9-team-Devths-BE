package com.ktb3.devths.user.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
	@NotBlank(message = "email은 필수입니다")
	String email,
	@NotBlank(message = "nickname은 필수입니다")
	@Size(min = 2, max = 10)
	String nickname,
	List<String> interests,
	@NotBlank(message = "tempToken은 필수입니다")
	String tempToken,
	String profileImageS3Key
) {
}
