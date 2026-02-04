package com.ktb3.devths.notification.dto.response;

public record UnreadCountResponse(
	Long unreadCount
) {
	public static UnreadCountResponse of(Long count) {
		return new UnreadCountResponse(count);
	}
}
