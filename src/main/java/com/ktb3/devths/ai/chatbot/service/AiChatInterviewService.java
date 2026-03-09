package com.ktb3.devths.ai.chatbot.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.chatbot.domain.constant.InterviewCompletionType;
import com.ktb3.devths.ai.chatbot.domain.constant.InterviewStatus;
import com.ktb3.devths.ai.chatbot.domain.constant.InterviewType;
import com.ktb3.devths.ai.chatbot.domain.constant.MessageRole;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.dto.internal.EvaluationPrepareResult;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiInterviewEvaluationRequest;
import com.ktb3.devths.ai.chatbot.dto.response.InterviewEndResponse;
import com.ktb3.devths.ai.chatbot.dto.response.InterviewStartResponse;
import com.ktb3.devths.ai.chatbot.repository.AiChatInterviewRepository;
import com.ktb3.devths.ai.chatbot.repository.AiChatMessageRepository;
import com.ktb3.devths.ai.client.FastApiClient;
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
public class AiChatInterviewService {

	private static final int MAX_EVALUATION_CONTEXT_PAIRS = 15;

	private final AiChatInterviewRepository aiChatInterviewRepository;
	private final AiChatMessageRepository aiChatMessageRepository;
	private final FastApiClient fastApiClient;
	private final AiChatStreamPersistService aiChatStreamPersistService;

	@Transactional
	public InterviewStartResponse startInterview(AiChatRoom room, InterviewType interviewType) {
		var existingInterview = aiChatInterviewRepository.findByRoomIdAndStatus(
			room.getId(), InterviewStatus.IN_PROGRESS);

		if (existingInterview.isPresent()) {
			AiChatInterview interview = existingInterview.get();

			if (interview.getInterviewType() != interviewType) {
				throw new CustomException(ErrorCode.INTERVIEW_TYPE_MISMATCH);
			}

			log.info("기존 진행 중인 면접 재사용: interviewId={}, roomId={}, type={}, currentCount={}",
				interview.getId(), room.getId(), interviewType, interview.getCurrentQuestionCount());

			return new InterviewStartResponse(
				interview.getId(),
				interview.getInterviewType().name(),
				interview.getCurrentQuestionCount(),
				true
			);
		}

		AiChatInterview interview = AiChatInterview.builder()
			.room(room)
			.interviewType(interviewType)
			.currentQuestionCount(0)
			.status(InterviewStatus.IN_PROGRESS)
			.build();

		AiChatInterview savedInterview = aiChatInterviewRepository.save(interview);
		log.info("새 면접 시작: interviewId={}, roomId={}, type={}", savedInterview.getId(), room.getId(), interviewType);

		return new InterviewStartResponse(
			savedInterview.getId(),
			savedInterview.getInterviewType().name(),
			savedInterview.getCurrentQuestionCount(),
			false
		);
	}

	@Transactional(readOnly = true)
	public Optional<AiChatInterview> getCurrentInterview(Long roomId) {
		return aiChatInterviewRepository.findByRoomIdAndStatus(roomId, InterviewStatus.IN_PROGRESS);
	}

	@Transactional
	public InterviewEndResponse endInterview(Long roomId, Long interviewId) {
		AiChatInterview interview = aiChatInterviewRepository.findById(interviewId)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_NOT_FOUND));

		if (!interview.getRoom().getId().equals(roomId)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		if (interview.getStatus() != InterviewStatus.COMPLETED) {
			interview.complete(InterviewCompletionType.MANUAL_END);
		}

		return new InterviewEndResponse(interview.getId(), interview.getStatus().name());
	}

	@Transactional(readOnly = true)
	public EvaluationPrepareResult prepareEvaluation(Long interviewId, boolean retry) {
		AiChatInterview interview = aiChatInterviewRepository.findById(interviewId)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_NOT_FOUND));

		if (interview.getCompletionType() == InterviewCompletionType.MANUAL_END) {
			throw new CustomException(ErrorCode.INTERVIEW_EVALUATION_NOT_ALLOWED);
		}
		AiChatRoom room = interview.getRoom();

		List<AiChatMessage> messages = aiChatMessageRepository.findByInterviewIdOrderByIdAsc(interviewId);

		if (messages.isEmpty()) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		List<FastApiInterviewEvaluationRequest.ContextEntry> allContext = new java.util.ArrayList<>();
		for (int i = 0; i < messages.size() - 1; i++) {
			AiChatMessage current = messages.get(i);
			AiChatMessage next = messages.get(i + 1);
			if (current.getRole() == MessageRole.ASSISTANT && next.getRole() == MessageRole.USER) {
				allContext.add(new FastApiInterviewEvaluationRequest.ContextEntry(
					current.getContent(),
					next.getContent()
				));
			}
		}

		int fromIndex = Math.max(0, allContext.size() - MAX_EVALUATION_CONTEXT_PAIRS);
		List<FastApiInterviewEvaluationRequest.ContextEntry> context = allContext.subList(fromIndex, allContext.size());

		Long roomId = room.getId();
		Long userId = room.getUser().getId();

		FastApiInterviewEvaluationRequest request = new FastApiInterviewEvaluationRequest(
			"면접 리포트 생성 (면접 종료)",
			new FastApiInterviewEvaluationRequest.Value(
				context,
				retry,
				roomId,
				interviewId,
				userId,
				interview.getInterviewType().name().toLowerCase()
			)
		);

		log.info("면접 평가 준비 완료: interviewId={}, roomId={}, userId={}, contextCount={}",
			interviewId, roomId, userId, context.size());

		return new EvaluationPrepareResult(interviewId, roomId, interview.getInterviewType(), request);
	}

	public Flux<String> streamEvaluation(EvaluationPrepareResult prepared) {
		StringBuilder fullEvaluation = new StringBuilder();
		AtomicBoolean hasError = new AtomicBoolean(false);

		return fastApiClient.streamInterviewEvaluation(prepared.request())
			.doOnNext(chunk -> {
				fullEvaluation.append(chunk);
				log.debug("평가 결과 청크 수신: length={}", chunk.length());
			})
			.doOnComplete(() -> {
				if (!hasError.get() && !fullEvaluation.isEmpty()) {
					Mono.fromRunnable(() -> {
						aiChatStreamPersistService.saveEvaluationResult(prepared.interviewId(), prepared.roomId(),
							prepared.interviewType(), fullEvaluation.toString());
						log.info("면접 평가 완료 및 저장: interviewId={}, evaluationLength={}",
							prepared.interviewId(), fullEvaluation.length());
					})
						.subscribeOn(Schedulers.boundedElastic())
						.doOnError(e -> log.error("면접 평가 완료 처리 실패: interviewId={}", prepared.interviewId(), e))
						.subscribe();
				}
			})
			.doOnError(e -> {
				hasError.set(true);
				log.error("면접 평가 스트리밍 실패: interviewId={}",
					LogSanitizer.sanitize(String.valueOf(prepared.interviewId())), e);
			});
	}
}
