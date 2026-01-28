package com.ktb3.devths.ai.analysis.dto.request;

import com.ktb3.devths.ai.constant.AiModel;

import jakarta.validation.constraints.NotNull;

public record DocumentAnalysisRequest(
	@NotNull(message = "AI 모델은 필수입니다")
	AiModel model,

	@NotNull(message = "이력서 정보는 필수입니다")
	DocumentInfo resume,

	@NotNull(message = "채용공고 정보는 필수입니다")
	DocumentInfo jobPost
) {
	public record DocumentInfo(
		Long fileId,
		String s3Key,
		String fileType,
		String text
	) {
		public boolean hasFileReference() {
			return fileId != null || s3Key != null;
		}
	}
}
