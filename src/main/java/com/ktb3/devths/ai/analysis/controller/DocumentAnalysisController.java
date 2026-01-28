package com.ktb3.devths.ai.analysis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.ai.analysis.dto.request.DocumentAnalysisRequest;
import com.ktb3.devths.ai.analysis.dto.response.DocumentAnalysisResponse;
import com.ktb3.devths.ai.analysis.service.DocumentAnalysisFacade;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ai-chatrooms")
@RequiredArgsConstructor
public class DocumentAnalysisController {

	private final DocumentAnalysisFacade documentAnalysisFacade;

	@PostMapping("/{roomId}/analysis")
	public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> startAnalysis(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable Long roomId,
		@Valid @RequestBody DocumentAnalysisRequest request
	) {
		long startTime = System.currentTimeMillis();
		Long userId = userPrincipal.getUserId();

		DocumentAnalysisResponse response = documentAnalysisFacade.startAnalysis(userId, roomId, request);

		long duration = System.currentTimeMillis() - startTime;
		log.info("분석 요청 응답 시간: {}ms (비동기 동작 확인: {}ms 미만이어야 정상)", duration, 1000);

		return ResponseEntity
			.status(HttpStatus.ACCEPTED)
			.body(ApiResponse.success("이력서 및 포트폴리오 분석이 시작되었습니다.", response));
	}
}
