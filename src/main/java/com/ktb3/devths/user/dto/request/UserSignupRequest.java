package com.ktb3.devths.user.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UserSignupRequest(
	@NotBlank(message = "email은 필수입니다")
	String email,
	@NotBlank(message = "nickname은 필수입니다")
	String nickname,
	@NotEmpty(message = "interests는 최소 1개 이상이어야 합니다")
	List<String> interests,
	@NotBlank(message = "tempToken은 필수입니다")
	String tempToken,
	String profileImageS3Key
) {
}
