package com.ktb3.devths.user.event;

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
public class FollowNotificationEventListener {

	private final NotificationService notificationService;
	private final UserRepository userRepository;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserFollowed(UserFollowedEvent event) {
		User recipient = userRepository.findByIdAndIsWithdrawFalse(event.followingId())
			.orElse(null);
		if (recipient == null) {
			log.warn("팔로우 알림 대상 사용자를 찾을 수 없습니다: followingId={}", event.followingId());
			return;
		}

		try {
			notificationService.createFollowNotification(
				recipient,
				event.followerId(),
				event.followerNickname()
			);
		} catch (Exception e) {
			log.warn(
				"팔로우 알림 생성 실패: followerId={}, followingId={}",
				event.followerId(),
				event.followingId(),
				e
			);
		}
	}
}
