package com.ktb3.devths.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.ktb3.devths.user.domain.entity.User;

public record UserUpdateResponse(
	String nickname,
	List<String> interests,
	LocalDateTime updatedAt
) {
	public static UserUpdateResponse of(User user, List<String> interestNames) {
		return new UserUpdateResponse(
			user.getNickname(),
			interestNames,
			user.getUpdatedAt()
		);
	}
}
