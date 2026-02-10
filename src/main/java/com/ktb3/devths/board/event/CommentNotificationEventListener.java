package com.ktb3.devths.board.event;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ktb3.devths.notification.service.NotificationService;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationEventListener {

	private final NotificationService notificationService;
	private final UserRepository userRepository;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePostCommentCreated(PostCommentCreatedEvent event) {
		Set<Long> recipientIds = new LinkedHashSet<>();
		recipientIds.add(event.postAuthorId());
		if (event.parentCommentAuthorId() != null) {
			recipientIds.add(event.parentCommentAuthorId());
		}
		recipientIds.remove(event.commenterId());

		if (recipientIds.isEmpty()) {
			return;
		}

		for (Long recipientId : recipientIds) {
			User recipient = userRepository.findByIdAndIsWithdrawFalse(recipientId)
				.orElse(null);
			if (recipient == null) {
				log.warn("댓글 알림 대상 사용자를 찾을 수 없습니다: recipientId={}", recipientId);
				continue;
			}

			try {
				if (event.parentCommentAuthorId() != null && recipientId.equals(event.parentCommentAuthorId())) {
					notificationService.createCommentReplyNotification(
						recipient,
						event.commenterId(),
						event.postId(),
						event.commenterNickname(),
						event.previewContent()
					);
					continue;
				}

				notificationService.createPostCommentNotification(
					recipient,
					event.commenterId(),
					event.postId(),
					event.commenterNickname(),
					event.previewContent()
				);
			} catch (Exception e) {
				log.warn(
					"댓글 알림 생성 실패: postId={}, commentId={}, commenterId={}, recipientId={}",
					event.postId(),
					event.commentId(),
					event.commenterId(),
					recipientId,
					e
				);
			}
		}
	}
}
