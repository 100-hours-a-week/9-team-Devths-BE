package com.ktb3.devths.user.dto.response;

import java.util.List;

import com.ktb3.devths.user.domain.entity.User;

public record UserMeResponse(
	String nickname,
	UserSignupResponse.ProfileImage profileImage,
	UserSignupResponse.UserStats stats,
	List<String> interests
) {
	public static UserMeResponse of(User user, List<String> interestNames) {
		return new UserMeResponse(
			user.getNickname(),
			null, // MVP에서 profileImage는 null
			new UserSignupResponse.UserStats(0, 0), // follower/following은 0
			interestNames
		);
	}
}
