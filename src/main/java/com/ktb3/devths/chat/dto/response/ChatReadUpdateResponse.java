package com.ktb3.devths.chat.dto.response;

public record ChatReadUpdateResponse(
	Long roomId,
	Long lastReadMsgId
) {
}
