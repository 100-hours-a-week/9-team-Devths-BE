package com.ktb3.devths.chat.dto.internal;

import com.ktb3.devths.chat.dto.response.ChatMessageResponse;

public record ChatMessageEnvelope(
	ChatMessageMeta meta,
	ChatTraceContext trace,
	ChatMessageResponse payload
) {
}
