package com.ktb3.devths.user.dto.internal;

import com.ktb3.devths.auth.dto.internal.TokenPair;
import com.ktb3.devths.user.dto.response.UserSignupResponse;

public record UserSignupResult(
	UserSignupResponse response,
	TokenPair tokenPair
) {
}
