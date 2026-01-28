package com.ktb3.devths.ai.analysis.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.ai.analysis.domain.AiOcrResult;
import com.ktb3.devths.ai.analysis.repository.AiOcrResultRepository;
import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOcrResultService {

	private final AiOcrResultRepository aiOcrResultRepository;

	@Transactional
	public AiOcrResult saveOcrResult(AiChatRoom room, String resumeOcr, String jobPostingOcr) {
		AiOcrResult ocrResult = aiOcrResultRepository.findByRoomId(room.getId())
			.orElse(null);

		if (ocrResult == null) {
			ocrResult = AiOcrResult.builder()
				.room(room)
				.resumeOcr(resumeOcr)
				.jobPostingOcr(jobPostingOcr)
				.build();

			log.info("새로운 OCR 결과 저장: roomId={}", room.getId());
		} else {
			ocrResult = AiOcrResult.builder()
				.id(ocrResult.getId())
				.room(room)
				.resumeOcr(resumeOcr)
				.jobPostingOcr(jobPostingOcr)
				.build();

			log.info("기존 OCR 결과 업데이트: roomId={}", room.getId());
		}

		return aiOcrResultRepository.save(ocrResult);
	}
}
