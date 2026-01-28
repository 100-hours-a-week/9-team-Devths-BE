package com.ktb3.devths.ai.chatbot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.chatbot.domain.constant.MessageRole;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiChatContext;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiChatRequest;
import com.ktb3.devths.ai.chatbot.repository.AiChatMessageRepository;
import com.ktb3.devths.ai.chatbot.repository.AiChatRoomRepository;
import com.ktb3.devths.ai.client.FastApiClient;
import com.ktb3.devths.ai.constant.AiModel;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.util.LogSanitizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatMessageService {

	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatRoomRepository aiChatRoomRepository;
	private final FastApiClient fastApiClient;

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

	public Flux<String> streamChatResponse(Long userId, Long roomId, String content, AiModel model) {
		log.info("AI 챗봇 스트리밍 시작: roomId={}, userId={}, model={}",
			LogSanitizer.sanitize(String.valueOf(roomId)),
			LogSanitizer.sanitize(String.valueOf(userId)),
			model);

		AiChatRoom room = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		if (!room.getUser().getId().equals(userId)) {
			throw new CustomException(ErrorCode.AI_CHATROOM_ACCESS_DENIED);
		}

		saveUserMessage(room, content);

		FastApiChatRequest request = new FastApiChatRequest(
			model.name().toLowerCase(),
			roomId,
			userId,
			content,
			null,
			FastApiChatContext.createNormalMode()
		);

		StringBuilder fullResponse = new StringBuilder();
		AtomicBoolean hasError = new AtomicBoolean(false);

		return fastApiClient.streamChatResponse(request)
			.doOnNext(chunk -> {
				fullResponse.append(chunk);
				log.debug("청크 수신: length={}", chunk.length());
			})
			.doOnComplete(() -> {
				if (!hasError.get()) {
					saveAssistantMessage(room, fullResponse.toString(), model);
					log.info("AI 챗봇 스트리밍 완료: roomId={}, totalLength={}",
						LogSanitizer.sanitize(String.valueOf(roomId)),
						fullResponse.length());
				}
			})
			.doOnError(e -> {
				hasError.set(true);
				log.error("AI 챗봇 스트리밍 실패: roomId={}", LogSanitizer.sanitize(String.valueOf(roomId)), e);

				if (fullResponse.length() > 0) {
					Map<String, Object> metadata = new HashMap<>();
					metadata.put("model", model.name());
					metadata.put("incomplete", true);
					metadata.put("error", e.getMessage());
					saveAssistantMessage(room, fullResponse.toString(), metadata);
				}
			})
			.onErrorResume(e -> Flux.just("ERROR:" + e.getMessage()));
	}

	@Transactional
	public AiChatMessage saveUserMessage(AiChatRoom room, String content) {
		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.role(MessageRole.USER)
			.type(MessageType.NORMAL)
			.content(content)
			.metadata(null)
			.build();

		return aiChatMessageRepository.save(message);
	}

	@Transactional
	public AiChatMessage saveAssistantMessage(AiChatRoom room, String content, AiModel model) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", model.name());

		return saveAssistantMessage(room, content, metadata);
	}

	@Transactional
	public AiChatMessage saveAssistantMessage(AiChatRoom room, String content, Map<String, Object> metadata) {
		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.role(MessageRole.ASSISTANT)
			.type(MessageType.NORMAL)
			.content(content)
			.metadata(metadata)
			.build();

		return aiChatMessageRepository.save(message);
	}
}
