package com.ktb3.devths.notification.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.notification.domain.constant.NotificationCategory;
import com.ktb3.devths.notification.domain.constant.NotificationType;
import com.ktb3.devths.notification.domain.entity.Notification;
import com.ktb3.devths.notification.dto.response.NotificationListResponse;
import com.ktb3.devths.notification.repository.NotificationRepository;
import com.ktb3.devths.user.domain.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;

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

	@Transactional
	public NotificationListResponse getNotificationList(Long userId, Integer size, Long lastId) {
		int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

		Pageable pageable = PageRequest.of(0, pageSize + 1);

		List<Notification> notifications = (lastId == null)
			? notificationRepository.findByRecipientIdAndNotDeleted(userId, pageable)
			: notificationRepository.findByRecipientIdAndNotDeletedAfterCursor(userId, lastId, pageable);

		if (!notifications.isEmpty()) {
			List<Long> notificationIds = notifications.stream()
				.limit(pageSize)
				.map(Notification::getId)
				.toList();

			notificationRepository.bulkUpdateReadStatus(notificationIds);
		}

		return NotificationListResponse.of(notifications, pageSize);
	}
}
