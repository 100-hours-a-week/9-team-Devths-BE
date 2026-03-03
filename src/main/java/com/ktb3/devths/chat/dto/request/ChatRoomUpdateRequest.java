package com.ktb3.devths.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatRoomUpdateRequest(
	String roomName,
	@NotNull(message = "알림 설정은 필수입니다")
	Boolean isAlarmOn
) {
}
