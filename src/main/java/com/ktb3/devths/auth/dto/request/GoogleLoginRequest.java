package com.ktb3.devths.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
	@NotBlank(message = "authCode는 필수입니다")
	String authCode
) {
}
