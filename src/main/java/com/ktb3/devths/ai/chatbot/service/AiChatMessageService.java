package com.ktb3.devths.ai.chatbot.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.analysis.domain.AiOcrResult;
import com.ktb3.devths.ai.analysis.repository.AiOcrResultRepository;
import com.ktb3.devths.ai.chatbot.domain.constant.InterviewStatus;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageRole;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageType;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.dto.internal.StreamPrepareResult;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatMessageService {

	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatRoomRepository aiChatRoomRepository;
	private final AiChatInterviewRepository aiChatInterviewRepository;
	private final AiOcrResultRepository aiOcrResultRepository;
	private final FastApiClient fastApiClient;
	private final AiChatStreamPersistService aiChatStreamPersistService;

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

	@Transactional
	public StreamPrepareResult prepareStreamContext(Long userId, Long roomId, String content,
		AiModel model, Long interviewId) {

		log.info("AI 챗봇 스트리밍 준비: roomId={}, userId={}, model={}, interviewId={}",
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

			if (interview.getStatus() == InterviewStatus.COMPLETED) {
				throw new CustomException(ErrorCode.INTERVIEW_COMPLETED);
			}
		}

		MessageType messageType = interviewId != null ? MessageType.INTERVIEW : MessageType.NORMAL;

		saveUserMessage(room, content, messageType, interview);

		if (interviewId != null) {
			AiOcrResult ocrResult = aiOcrResultRepository.findByRoomId(roomId).orElse(null);
			String resumeOcr = ocrResult != null ? ocrResult.getResumeOcr() : "";
			String jobPostingOcr = ocrResult != null ? ocrResult.getJobPostingOcr() : "";

			context = new FastApiChatContext(
				MessageType.INTERVIEW.name().toLowerCase(),
				resumeOcr,
				jobPostingOcr,
				interview.getInterviewType().name().toLowerCase(),
				interview.getCurrentQuestionCount() + 1
			);
		}

		FastApiChatRequest request = new FastApiChatRequest(
			model.name().toLowerCase(),
			roomId,
			userId,
			content,
			interviewId,
			context
		);

		return new StreamPrepareResult(roomId, interviewId, messageType, model, request);
	}

	public Flux<String> streamChatResponse(StreamPrepareResult prepared) {
		StringBuilder fullResponse = new StringBuilder();
		AtomicBoolean hasError = new AtomicBoolean(false);
		AtomicBoolean isFastApiError = new AtomicBoolean(false);

		return fastApiClient.streamChatResponse(prepared.request())
			.doOnNext(chunk -> {
				if (chunk.startsWith("[ERROR]")) {
					isFastApiError.set(true);
					hasError.set(true);

					String fallbackMessage = chunk.substring("[ERROR]".length());
					fullResponse.append(fallbackMessage);

					log.warn("FastAPI 에러 청크 감지: roomId={}, interviewId={}, fallback='{}'",
						prepared.roomId(), prepared.interviewId(), fallbackMessage);
				} else {
					fullResponse.append(chunk);
				}

				log.debug("청크 수신: length={}", chunk.length());
			})
			.filter(chunk -> !chunk.startsWith("[ERROR]"))
			.doOnComplete(() -> {
				if (!hasError.get()) {
					Mono.fromRunnable(() -> {
						aiChatStreamPersistService.saveStreamResult(prepared.roomId(), prepared.interviewId(),
							fullResponse.toString(), prepared.model(), prepared.messageType(), false);
						log.info("AI 챗봇 스트리밍 완료: roomId={}, totalLength={}", LogSanitizer.sanitize(String.valueOf(prepared.roomId())), fullResponse.length());
					})
						.subscribeOn(Schedulers.boundedElastic())
						.doOnError(e -> log.error("어시스턴트 메시지 저장 실패: roomId={}, length={}",
							LogSanitizer.sanitize(String.valueOf(prepared.roomId())),
							fullResponse.length(), e))
						.subscribe();
				} else if (isFastApiError.get()) {
					Mono.fromRunnable(() -> {
						aiChatStreamPersistService.saveStreamResult(prepared.roomId(), prepared.interviewId(),
							fullResponse.toString(), prepared.model(), prepared.messageType(), true);
						log.warn("FastAPI 에러 응답 저장 완료 (질문 개수 증가 안 함): roomId={}, interviewId={}", prepared.roomId(), prepared.interviewId());
					})
						.subscribeOn(Schedulers.boundedElastic())
						.doOnError(e -> log.error("FastAPI 에러 응답 저장 실패", e))
						.subscribe();
				}
			})
			.doOnError(e -> {
				hasError.set(true);
				log.error("AI 챗봇 스트리밍 실패: roomId={}",
					LogSanitizer.sanitize(String.valueOf(prepared.roomId())), e);

				if (!fullResponse.isEmpty()) {
					Mono.fromRunnable(() ->
							aiChatStreamPersistService.savePartialResult(prepared.roomId(), prepared.interviewId(),
								fullResponse.toString(), prepared.model(), prepared.messageType(), e.getMessage())
						)
						.subscribeOn(Schedulers.boundedElastic())
						.doOnError(ex -> log.error("부분 응답 저장 실패: roomId={}",
							LogSanitizer.sanitize(String.valueOf(prepared.roomId())), ex))
						.subscribe();
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
}
