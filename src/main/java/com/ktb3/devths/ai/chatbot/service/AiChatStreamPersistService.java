package com.ktb3.devths.ai.chatbot.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.chatbot.domain.constant.InterviewCompletionType;
import com.ktb3.devths.ai.chatbot.domain.constant.InterviewType;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageRole;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.repository.AiChatInterviewRepository;
import com.ktb3.devths.ai.chatbot.repository.AiChatMessageRepository;
import com.ktb3.devths.ai.chatbot.repository.AiChatRoomRepository;
import com.ktb3.devths.ai.constant.AiModel;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatStreamPersistService {

	private final AiChatRoomRepository aiChatRoomRepository;
	private final AiChatInterviewRepository aiChatInterviewRepository;
	private final AiChatMessageRepository aiChatMessageRepository;

	@Transactional
	public void saveStreamResult(Long roomId, Long interviewId, String content,
		AiModel model, MessageType messageType, boolean isFastApiError) {
		AiChatRoom room = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		AiChatInterview interview = interviewId != null
			? aiChatInterviewRepository.findById(interviewId).orElse(null)
			: null;

		if (interview != null && !isFastApiError) {
			interview.incrementQuestionCount();
			log.info("면접 질문 개수 증가: interviewId={}, count={}",
				interview.getId(), interview.getCurrentQuestionCount());
		}

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", model.name());
		if (isFastApiError) {
			metadata.put("fastapi_error", true);
			metadata.put("error_type", "PARSE_FAILED");
		}

		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.interview(interview)
			.role(MessageRole.ASSISTANT)
			.type(messageType)
			.content(content)
			.metadata(metadata)
			.build();

		aiChatMessageRepository.save(message);
	}

	@Transactional
	public void savePartialResult(Long roomId, Long interviewId, String content,
		AiModel model, MessageType messageType, String errorMessage) {
		AiChatRoom room = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		AiChatInterview interview = interviewId != null
			? aiChatInterviewRepository.findById(interviewId).orElse(null)
			: null;

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", model.name());
		metadata.put("incomplete", true);
		metadata.put("error", errorMessage);

		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.interview(interview)
			.role(MessageRole.ASSISTANT)
			.type(messageType)
			.content(content)
			.metadata(metadata)
			.build();

		aiChatMessageRepository.save(message);
	}

	@Transactional
	public void saveEvaluationResult(Long interviewId, Long roomId,
		InterviewType interviewType, String evaluationContent) {
		AiChatRoom room = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		AiChatInterview interview = aiChatInterviewRepository.findById(interviewId)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_NOT_FOUND));

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("interview_type", interviewType.name());
		metadata.put("evaluation", true);

		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.interview(interview)
			.role(MessageRole.ASSISTANT)
			.type(MessageType.INTERVIEW)
			.content(evaluationContent)
			.metadata(metadata)
			.build();

		aiChatMessageRepository.save(message);
		interview.complete(InterviewCompletionType.EVALUATION);
	}
}
