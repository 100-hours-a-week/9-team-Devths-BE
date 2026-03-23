package com.ktb3.devths.chat.dto.internal;

import com.ktb3.devths.chat.dto.response.ChatRoomNotification;

public record ChatNotificationEnvelope(
	ChatNotificationMeta meta,
	ChatTraceContext trace,
	ChatRoomNotification payload
) {
}
