package com.ktb3.devths.auth.dto.response;

import java.util.List;

public record GoogleLoginResponse(
	boolean isRegistered,
	String email,
	String tempToken,
	String nickname,
	LoginResponse.ProfileImage profileImage,
	LoginResponse.UserStats stats,
	List<String> interests
) {
	public static GoogleLoginResponse registered(LoginResponse loginResponse) {
		return new GoogleLoginResponse(
			true,
			null,
			null,
			loginResponse.nickname(),
			loginResponse.profileImage(),
			loginResponse.stats(),
			loginResponse.interests()
		);
	}

	public static GoogleLoginResponse newUser(String email, String tempToken) {
		return new GoogleLoginResponse(false, email, tempToken, null, null, null, null);
	}
}
