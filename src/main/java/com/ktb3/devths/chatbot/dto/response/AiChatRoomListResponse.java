package com.ktb3.devths.chatbot.dto.response;

import java.util.List;

import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;

public record AiChatRoomListResponse(
	List<AiChatRoomResponse> rooms,
	Long lastId,
	boolean hasNext
) {
	public static AiChatRoomListResponse of(List<AiChatRoom> chatRooms, int requestedSize) {
		boolean hasNext = chatRooms.size() > requestedSize;

		List<AiChatRoom> actualRooms = hasNext
			? chatRooms.subList(0, requestedSize)
			: chatRooms;

		List<AiChatRoomResponse> rooms = actualRooms.stream()
			.map(AiChatRoomResponse::from)
			.toList();

		Long lastId = rooms.isEmpty() ? null : rooms.get(rooms.size() - 1).roomId();

		return new AiChatRoomListResponse(rooms, lastId, hasNext);
	}
}
