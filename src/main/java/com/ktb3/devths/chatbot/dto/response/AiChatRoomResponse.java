package com.ktb3.devths.chatbot.dto.response;

import java.time.LocalDateTime;

import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;

public record AiChatRoomResponse(
	Long roomId,
	String roomUuid,
	String title,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static AiChatRoomResponse from(AiChatRoom room) {
		return new AiChatRoomResponse(
			room.getId(),
			room.getRoomUuid(),
			room.getTitle(),
			room.getCreatedAt(),
			room.getUpdatedAt()
		);
	}
}
