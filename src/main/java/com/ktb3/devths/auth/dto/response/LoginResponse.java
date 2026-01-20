package com.ktb3.devths.auth.dto.response;

import java.util.List;

import com.ktb3.devths.user.domain.entity.User;

public record LoginResponse(
	String nickname,
	ProfileImage profileImage,
	UserStats stats,
	List<String> interests
) {
	public static LoginResponse of(User user, List<String> interests) {
		return new LoginResponse(
			user.getNickname(),
			null, // profileImage는 MVP에서 제외
			new UserStats(0, 0), // followerCount, followingCount (MVP에서 0)
			interests
		);
	}

	public record ProfileImage(Long id, String url) {
	}

	public record UserStats(int followerCount, int followingCount) {
	}
}