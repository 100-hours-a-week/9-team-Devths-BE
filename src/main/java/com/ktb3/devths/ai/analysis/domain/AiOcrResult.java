package com.ktb3.devths.ai.analysis.domain;

import com.ktb3.devths.ai.chatbot.domain.entity.AiChatRoom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(name = "ai_ocr_result")
public class AiOcrResult {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private AiChatRoom room;

	@Column(name = "resume_ocr", nullable = false)
	private String resumeOcr;

	@Column(name = "job_posting_ocr", nullable = false)
	private String jobPostingOcr;
}
