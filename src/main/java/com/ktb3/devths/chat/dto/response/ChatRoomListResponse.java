package com.ktb3.devths.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomListResponse(
	List<ChatRoomSummaryResponse> chatRooms,
	LocalDateTime cursor,
	boolean hasNext
) {
}
