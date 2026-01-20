package com.ktb3.devths.auth.dto.internal;

import com.ktb3.devths.auth.dto.response.LoginResponse;

public record LoginResult(
	LoginResponse loginResponse,
	TokenPair tokenPair
) {
}
