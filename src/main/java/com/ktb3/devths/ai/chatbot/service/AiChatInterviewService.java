package com.ktb3.devths.ai.chatbot.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.analysis.repository.AiOcrResultRepository;
import com.ktb3.devths.ai.chatbot.domain.constant.InterviewStatus;
import com.ktb3.devths.ai.chatbot.domain.constant.InterviewType;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiInterviewEvaluationRequest;
import com.ktb3.devths.ai.chatbot.repository.AiChatInterviewRepository;
import com.ktb3.devths.ai.chatbot.repository.AiChatMessageRepository;
import com.ktb3.devths.ai.client.FastApiClient;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatInterviewService {

	private final AiChatInterviewRepository aiChatInterviewRepository;
	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiOcrResultRepository aiOcrResultRepository;
	private final FastApiClient fastApiClient;

	@Transactional
	public AiChatInterview startInterview(AiChatRoom room, InterviewType interviewType) {
		aiChatInterviewRepository.findByRoomIdAndStatus(room.getId(), InterviewStatus.IN_PROGRESS)
			.ifPresent(interview -> {
				throw new CustomException(ErrorCode.INTERVIEW_ALREADY_IN_PROGRESS);
			});

		AiChatInterview interview = AiChatInterview.builder()
			.room(room)
			.interviewType(interviewType)
			.currentQuestionCount(0)
			.status(InterviewStatus.IN_PROGRESS)
			.build();

		AiChatInterview savedInterview = aiChatInterviewRepository.save(interview);
		log.info("면접 시작: interviewId={}, roomId={}, type={}", savedInterview.getId(), room.getId(), interviewType);

		return savedInterview;
	}

	@Transactional
	public void completeInterview(Long interviewId) {
		AiChatInterview interview = aiChatInterviewRepository.findById(interviewId)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_NOT_FOUND));

		interview.complete();
		log.info("면접 완료: interviewId={}", interviewId);
	}

	@Transactional(readOnly = true)
	public AiChatInterview getInterview(Long interviewId) {
		return aiChatInterviewRepository.findById(interviewId)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_NOT_FOUND));
	}

	public Flux<String> evaluateInterview(Long interviewId) {
		// 현재 스레드의 Authentication 캡처 (여기서는 SecurityContext가 존재함)
		Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

		AiChatInterview interview = getInterview(interviewId);

		List<AiChatMessage> messages = aiChatMessageRepository.findAll().stream()
			.filter(msg -> msg.getInterview() != null && msg.getInterview().getId().equals(interviewId))
			.collect(Collectors.toList());

		if (messages.isEmpty()) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		List<FastApiInterviewEvaluationRequest.FastApiInterviewMessage> interviewMessages = messages.stream()
			.map(msg -> new FastApiInterviewEvaluationRequest.FastApiInterviewMessage(
				msg.getRole().name().toLowerCase(),
				msg.getContent()
			))
			.collect(Collectors.toList());

		// roomId와 userId 추출
		Long roomId = interview.getRoom().getId();
		Long userId = interview.getRoom().getUser().getId();

		FastApiInterviewEvaluationRequest request = new FastApiInterviewEvaluationRequest(
			interviewId,
			interview.getInterviewType().name().toLowerCase(),
			roomId,
			userId,
			interviewMessages
		);

		log.info("면접 평가 시작: interviewId={}, roomId={}, userId={}, messageCount={}",
			interviewId, roomId, userId, messages.size());

		return fastApiClient.streamInterviewEvaluation(request)
			.doOnComplete(() -> {
				try {
					// SecurityContext 복원
					SecurityContextHolder.getContext().setAuthentication(currentAuth);

					completeInterview(interviewId);
					log.info("면접 평가 완료: interviewId={}", interviewId);
				} catch (Exception e) {
					log.error("면접 완료 처리 실패: interviewId={}", interviewId, e);
				} finally {
					// SecurityContext 정리 (메모리 누수 방지)
					SecurityContextHolder.clearContext();
				}
			});
	}
}
