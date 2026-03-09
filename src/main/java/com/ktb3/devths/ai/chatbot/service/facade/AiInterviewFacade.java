package com.ktb3.devths.ai.chatbot.service.facade;

import org.springframework.stereotype.Service;

import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.ai.chatbot.dto.request.InterviewEndRequest;
import com.ktb3.devths.ai.chatbot.dto.request.InterviewStartRequest;
import com.ktb3.devths.ai.chatbot.dto.response.CurrentInterviewResponse;
import com.ktb3.devths.ai.chatbot.dto.response.InterviewEndResponse;
import com.ktb3.devths.ai.chatbot.dto.response.InterviewStartResponse;
import com.ktb3.devths.ai.chatbot.service.AiChatInterviewService;
import com.ktb3.devths.ai.chatbot.service.AiChatRoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiInterviewFacade {

	private final AiChatRoomService aiChatRoomService;
	private final AiChatInterviewService aiChatInterviewService;

	public CurrentInterviewResponse getCurrentInterview(Long userId, Long roomId) {
		aiChatRoomService.getOwnedRoomOrThrow(userId, roomId);

		return aiChatInterviewService.getCurrentInterview(roomId)
			.map(CurrentInterviewResponse::from)
			.orElse(null);
	}

	public InterviewStartFacadeResult startInterview(Long userId, Long roomId, InterviewStartRequest request) {
		AiChatRoom room = aiChatRoomService.getOwnedRoomOrThrow(userId, roomId);
		InterviewStartResponse response = aiChatInterviewService.startInterview(room, request.interviewType());

		String message = response.isResumed() ? "기존 면접을 계속 진행합니다." : "면접이 시작되었습니다.";
		return new InterviewStartFacadeResult(message, response);
	}

	public InterviewEndResponse endInterview(Long userId, Long roomId, InterviewEndRequest request) {
		aiChatRoomService.getOwnedRoomOrThrow(userId, roomId);
		return aiChatInterviewService.endInterview(roomId, request.interviewId());
	}

	public record InterviewStartFacadeResult(String message, InterviewStartResponse response) {
	}
}
