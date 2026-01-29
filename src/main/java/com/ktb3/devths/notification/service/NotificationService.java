package com.ktb3.devths.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.notification.domain.constant.NotificationCategory;
import com.ktb3.devths.notification.domain.constant.NotificationType;
import com.ktb3.devths.notification.domain.entity.Notification;
import com.ktb3.devths.notification.repository.NotificationRepository;
import com.ktb3.devths.user.domain.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;

	@Transactional
	public Notification createAnalysisCompleteNotification(User recipient, Long roomId, String summary) {
		Notification notification = Notification.builder()
			.recipient(recipient)
			.sender(null)
			.category(NotificationCategory.AI)
			.type(NotificationType.REPORT)
			.content(summary != null ? summary : "이력서 분석이 완료되었습니다")
			.targetPath("/ai/chat/" + roomId)
			.resourceId(roomId)
			.isRead(false)
			.build();

		Notification savedNotification = notificationRepository.save(notification);
		log.info("분석 완료 알림 생성: recipientId={}, roomId={}", recipient.getId(), roomId);

		return savedNotification;
	}
}
