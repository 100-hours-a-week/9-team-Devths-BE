package com.ktb3.devths.ai.chatbot.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.chatbot.domain.constant.MessageRole;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.repository.AiChatMessageRepository;
import com.ktb3.devths.ai.chatbot.repository.AiChatRoomRepository;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatMessageService {

	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatRoomRepository aiChatRoomRepository;

	@Transactional
	public AiChatMessage saveReportMessage(Long roomId, String content, Map<String, Object> metadata) {
		AiChatRoom room = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.role(MessageRole.ASSISTANT)
			.type(MessageType.REPORT)
			.content(content)
			.metadata(metadata)
			.build();

		return aiChatMessageRepository.save(message);
	}
}
