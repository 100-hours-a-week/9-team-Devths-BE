package com.ktb3.devths.ai.chatbot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.analysis.domain.AiOcrResult;
import com.ktb3.devths.ai.analysis.repository.AiOcrResultRepository;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageRole;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiChatContext;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiChatRequest;
import com.ktb3.devths.ai.chatbot.repository.AiChatInterviewRepository;
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
	private final AiChatInterviewRepository aiChatInterviewRepository;
	private final AiOcrResultRepository aiOcrResultRepository;
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

	public Flux<String> streamChatResponse(Long userId, Long roomId, String content, AiModel model,
		Long interviewId) {
		// 현재 스레드의 Authentication 캡처 (여기서는 SecurityContext가 존재함)
		Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

		log.info("AI 챗봇 스트리밍 시작: roomId={}, userId={}, model={}, interviewId={}",
			LogSanitizer.sanitize(String.valueOf(roomId)),
			LogSanitizer.sanitize(String.valueOf(userId)),
			model,
			interviewId);

		AiChatRoom room = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		if (!room.getUser().getId().equals(userId)) {
			throw new CustomException(ErrorCode.AI_CHATROOM_ACCESS_DENIED);
		}

		AiChatInterview interview = null;
		FastApiChatContext context = FastApiChatContext.createNormalMode();

		if (interviewId != null) {
			interview = aiChatInterviewRepository.findById(interviewId)
				.orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_NOT_FOUND));

			// 5개 질문 제한 체크
			if (interview.getCurrentQuestionCount() > 5) {
				interview.complete();
				throw new CustomException(ErrorCode.INTERVIEW_COMPLETED_EVALUATION_REQUIRED);
			}

			interview.incrementQuestionCount();

			AiOcrResult ocrResult = aiOcrResultRepository.findByRoomId(roomId).orElse(null);
			String resumeOcr = ocrResult != null ? ocrResult.getResumeOcr() : "";
			String jobPostingOcr = ocrResult != null ? ocrResult.getJobPostingOcr() : "";

			context = new FastApiChatContext(
				MessageType.INTERVIEW.name().toLowerCase(),
				resumeOcr,
				jobPostingOcr,
				interview.getInterviewType().name().toLowerCase(),
				interview.getCurrentQuestionCount()
			);
		}

		MessageType messageType = interviewId != null ? MessageType.INTERVIEW : MessageType.NORMAL;
		AiChatInterview finalInterview = interview;

		saveUserMessage(room, content, messageType, interview);

		FastApiChatRequest request = new FastApiChatRequest(
			model.name().toLowerCase(),
			roomId,
			userId,
			content,
			interviewId,
			context
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
					try {
						// SecurityContext 복원
						SecurityContextHolder.getContext().setAuthentication(currentAuth);

						saveAssistantMessage(room, fullResponse.toString(), model, messageType, finalInterview);
						log.info("AI 챗봇 스트리밍 완료: roomId={}, totalLength={}",
							LogSanitizer.sanitize(String.valueOf(roomId)),
							fullResponse.length());
					} catch (Exception e) {
						log.error("어시스턴트 메시지 저장 실패: roomId={}, length={}",
							LogSanitizer.sanitize(String.valueOf(roomId)),
							fullResponse.length(),
							e);
					} finally {
						// SecurityContext 정리 (메모리 누수 방지)
						SecurityContextHolder.clearContext();
					}
				}
			})
			.doOnError(e -> {
				hasError.set(true);
				log.error("AI 챗봇 스트리밍 실패: roomId={}", LogSanitizer.sanitize(String.valueOf(roomId)), e);

				if (fullResponse.length() > 0) {
					try {
						// SecurityContext 복원
						SecurityContextHolder.getContext().setAuthentication(currentAuth);

						Map<String, Object> metadata = new HashMap<>();
						metadata.put("model", model.name());
						metadata.put("incomplete", true);
						metadata.put("error", e.getMessage());
						saveAssistantMessage(room, fullResponse.toString(), metadata, messageType, finalInterview);
					} catch (Exception ex) {
						log.error("부분 응답 저장 실패: roomId={}",
							LogSanitizer.sanitize(String.valueOf(roomId)), ex);
					} finally {
						SecurityContextHolder.clearContext();
					}
				}
			})
			.onErrorResume(e -> Flux.just("ERROR:" + e.getMessage()));
	}

	@Transactional
	public AiChatMessage saveUserMessage(AiChatRoom room, String content, MessageType type,
		AiChatInterview interview) {
		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.interview(interview)
			.role(MessageRole.USER)
			.type(type)
			.content(content)
			.metadata(null)
			.build();

		return aiChatMessageRepository.save(message);
	}

	@Transactional
	public AiChatMessage saveAssistantMessage(AiChatRoom room, String content, AiModel model, MessageType type,
		AiChatInterview interview) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", model.name());

		return saveAssistantMessage(room, content, metadata, type, interview);
	}

	@Transactional
	public AiChatMessage saveAssistantMessage(AiChatRoom room, String content, Map<String, Object> metadata,
		MessageType type, AiChatInterview interview) {
		AiChatMessage message = AiChatMessage.builder()
			.room(room)
			.interview(interview)
			.role(MessageRole.ASSISTANT)
			.type(type)
			.content(content)
			.metadata(metadata)
			.build();

		return aiChatMessageRepository.save(message);
	}
}
