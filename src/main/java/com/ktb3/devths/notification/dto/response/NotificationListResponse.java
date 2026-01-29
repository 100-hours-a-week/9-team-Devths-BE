package com.ktb3.devths.notification.dto.response;

import java.util.List;

import com.ktb3.devths.notification.domain.entity.Notification;

public record NotificationListResponse(
	List<NotificationResponse> notifications,
	Long lastId,
	boolean hasNext
) {
	public static NotificationListResponse of(List<Notification> notificationList, int requestedSize) {
		boolean hasNext = notificationList.size() > requestedSize;

		List<Notification> actualNotifications = hasNext
			? notificationList.subList(0, requestedSize)
			: notificationList;

		List<NotificationResponse> notifications = actualNotifications.stream()
			.map(NotificationResponse::from)
			.toList();

		Long lastId = notifications.isEmpty() ? null : notifications.get(notifications.size() - 1).notificationId();

		return new NotificationListResponse(notifications, lastId, hasNext);
	}
}
