package com.ktb3.devths.user.event;

public record UserFollowedEvent(
	Long followerId,
	Long followingId,
	String followerNickname
) {
}
