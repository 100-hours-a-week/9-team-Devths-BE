package com.ktb3.devths.board.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BoardEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public void publishPostCommentCreated(
		Long commentId,
		Long postId,
		Long postAuthorId,
		Long parentCommentId,
		Long parentCommentAuthorId,
		Long commenterId,
		String commenterNickname,
		String previewContent
	) {
		applicationEventPublisher.publishEvent(
			new PostCommentCreatedEvent(
				commentId,
				postId,
				postAuthorId,
				parentCommentId,
				parentCommentAuthorId,
				commenterId,
				commenterNickname,
				previewContent
			)
		);
	}
}
