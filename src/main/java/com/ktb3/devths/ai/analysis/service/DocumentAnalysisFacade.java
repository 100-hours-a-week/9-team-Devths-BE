package com.ktb3.devths.ai.analysis.service;

import org.springframework.stereotype.Service;

import com.ktb3.devths.ai.analysis.dto.request.DocumentAnalysisRequest;
import com.ktb3.devths.ai.analysis.dto.response.DocumentAnalysisResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentAnalysisFacade {

	private final DocumentAnalysisService documentAnalysisService;
	private final AsyncAnalysisProcessor asyncAnalysisProcessor;

	public DocumentAnalysisResponse startAnalysis(Long userId, Long roomId, DocumentAnalysisRequest request) {
		DocumentAnalysisResponse response = documentAnalysisService.startAnalysis(userId, roomId, request);
		asyncAnalysisProcessor.processAnalysis(response.taskId(), userId, roomId, request);
		return response;
	}
}
