package com.ktb3.devths.user.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public void publishFollowed(Long followerId, Long followingId, String followerNickname) {
		applicationEventPublisher.publishEvent(
			new UserFollowedEvent(followerId, followingId, followerNickname)
		);
	}
}
