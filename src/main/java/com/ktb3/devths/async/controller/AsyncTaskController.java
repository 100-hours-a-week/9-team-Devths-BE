package com.ktb3.devths.async.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.async.dto.response.AsyncTaskResponse;
import com.ktb3.devths.async.service.AsyncTaskQueryService;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

	private final AsyncTaskQueryService asyncTaskQueryService;

	@GetMapping("/{taskId}")
	public ResponseEntity<ApiResponse<AsyncTaskResponse>> getTaskStatus(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable Long taskId
	) {
		Long userId = userPrincipal.getUserId();

		AsyncTaskResponse response = asyncTaskQueryService.getTaskStatus(userId, taskId);

		String message = switch (response.status()) {
			case "COMPLETED" -> "이력서 및 포트폴리오 분석에 성공하였습니다.";
			case "FAILED" -> "이력서 분석에 실패하였습니다.";
			default -> "비동기 작업이 처리 중입니다.";
		};

		HttpStatus status = "COMPLETED".equals(response.status()) || "FAILED".equals(response.status())
			? HttpStatus.OK
			: HttpStatus.ACCEPTED;

		return ResponseEntity
			.status(status)
			.body(ApiResponse.success(message, response));
	}
}
