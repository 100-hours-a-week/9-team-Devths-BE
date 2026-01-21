package com.ktb3.devths.global.storage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;
import com.ktb3.devths.global.storage.dto.request.PresignedUrlRequest;
import com.ktb3.devths.global.storage.dto.response.PresignedUrlResponse;
import com.ktb3.devths.global.storage.service.S3StorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
	private final S3StorageService s3StorageService;

	@PostMapping("/presigned")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@Valid @RequestBody PresignedUrlRequest request
	) {
		PresignedUrlResponse response = s3StorageService.generatePresignedUrl(request);

		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.success("Presigned URL이 생성되었습니다.", response));
	}
}
