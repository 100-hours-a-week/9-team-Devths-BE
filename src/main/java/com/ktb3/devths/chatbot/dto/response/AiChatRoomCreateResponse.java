package com.ktb3.devths.chatbot.dto.response;

import java.time.LocalDateTime;

import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;

public record AiChatRoomCreateResponse(
	Long roomId,
	String roomUuid,
	String title,
	LocalDateTime createdAt
) {
	public static AiChatRoomCreateResponse from(AiChatRoom room) {
		return new AiChatRoomCreateResponse(
			room.getId(),
			room.getRoomUuid(),
			room.getTitle(),
			room.getCreatedAt()
		);
	}
}
