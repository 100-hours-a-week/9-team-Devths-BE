package com.ktb3.devths.auth.dto.internal;

import com.ktb3.devths.auth.dto.response.LoginResponse;

public record GoogleLoginResult(
	boolean isRegistered,
	LoginResponse loginResponse,
	TokenPair tokenPair,
	String email,
	String tempToken
) {
	public static GoogleLoginResult registered(LoginResponse loginResponse, TokenPair tokenPair) {
		return new GoogleLoginResult(true, loginResponse, tokenPair, null, null);
	}

	public static GoogleLoginResult newUser(String email, String tempToken) {
		return new GoogleLoginResult(false, null, null, email, tempToken);
	}
}
