package com.ktb3.devths.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatReadUpdateRequest(
	@NotNull Long lastReadMsgId
) {
}
