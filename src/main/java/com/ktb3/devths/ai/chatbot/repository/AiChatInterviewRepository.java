package com.ktb3.devths.ai.chatbot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.ai.chatbot.domain.constant.InterviewStatus;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatInterview;

public interface AiChatInterviewRepository extends JpaRepository<AiChatInterview, Long> {
	Optional<AiChatInterview> findByRoomIdAndStatus(Long roomId, InterviewStatus status);
}
