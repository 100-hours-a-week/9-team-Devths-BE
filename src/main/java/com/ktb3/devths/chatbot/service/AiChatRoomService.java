package com.ktb3.devths.chatbot.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.chatbot.dto.response.AiChatRoomListResponse;
import com.ktb3.devths.chatbot.repository.AiChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatRoomService {

	private static final int DEFAULT_PAGE_SIZE = 10;

	private final AiChatRoomRepository aiChatRoomRepository;

	@Transactional(readOnly = true)
	public AiChatRoomListResponse getChatRoomList(Long userId, Integer size, Long lastId) {
		int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;

		Pageable pageable = PageRequest.of(0, pageSize + 1);

		List<AiChatRoom> chatRooms = (lastId == null)
			? aiChatRoomRepository.findByUserIdAndNotDeleted(userId, pageable)
			: aiChatRoomRepository.findByUserIdAndNotDeletedAfterCursor(userId, lastId, pageable);

		return AiChatRoomListResponse.of(chatRooms, pageSize);
	}
}
