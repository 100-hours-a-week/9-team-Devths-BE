package com.ktb3.devths.notification.dto.response;

import java.time.LocalDateTime;

import com.ktb3.devths.notification.domain.entity.Notification;

public record NotificationResponse(
	Long notificationId,
	SenderInfo sender,
	String category,
	String type,
	String content,
	String targetPath,
	Long resourceId,
	LocalDateTime createdAt,
	boolean isRead
) {
	public static NotificationResponse from(Notification notification) {
		SenderInfo senderInfo = notification.getSender() != null
			? new SenderInfo(
			notification.getSender().getId(),
			notification.getSender().getNickname()
		)
			: null;

		return new NotificationResponse(
			notification.getId(),
			senderInfo,
			notification.getCategory().name(),
			notification.getType().name(),
			notification.getContent(),
			notification.getTargetPath(),
			notification.getResourceId(),
			notification.getCreatedAt(),
			notification.isRead()
		);
	}

	public record SenderInfo(
		Long senderId,
		String senderName
	) {
	}
}
