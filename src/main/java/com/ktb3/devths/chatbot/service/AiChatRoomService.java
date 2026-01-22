package com.ktb3.devths.chatbot.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.chatbot.dto.response.AiChatRoomCreateResponse;
import com.ktb3.devths.chatbot.dto.response.AiChatRoomListResponse;
import com.ktb3.devths.chatbot.repository.AiChatRoomRepository;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatRoomService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final String DEFAULT_TITLE = "새 채팅방";

	private final AiChatRoomRepository aiChatRoomRepository;
	private final UserRepository userRepository;

	@Transactional
	public AiChatRoomCreateResponse createChatRoom(Long userId) {
		User user = userRepository.findByIdAndIsWithdrawFalse(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		String roomUuid = java.util.UUID.randomUUID().toString();

		AiChatRoom chatRoom = AiChatRoom.builder()
			.user(user)
			.roomUuid(roomUuid)
			.title(DEFAULT_TITLE)
			.isDeleted(false)
			.build();

		AiChatRoom savedChatRoom = aiChatRoomRepository.save(chatRoom);

		return AiChatRoomCreateResponse.from(savedChatRoom);
	}

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
