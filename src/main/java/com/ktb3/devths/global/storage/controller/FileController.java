package com.ktb3.devths.global.storage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;
import com.ktb3.devths.global.storage.dto.request.FileAttachmentRequest;
import com.ktb3.devths.global.storage.dto.request.PresignedUrlRequest;
import com.ktb3.devths.global.storage.dto.response.FileAttachmentResponse;
import com.ktb3.devths.global.storage.dto.response.PresignedUrlResponse;
import com.ktb3.devths.global.storage.service.S3AttachmentService;
import com.ktb3.devths.global.storage.service.S3StorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
	private final S3StorageService s3StorageService;
	private final S3AttachmentService s3AttachmentService;

	@PostMapping("/presigned")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@Valid @RequestBody PresignedUrlRequest request
	) {
		PresignedUrlResponse response = s3StorageService.generatePresignedUrl(request);

		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.success("Presigned URL이 생성되었습니다.", response));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<FileAttachmentResponse>> saveAttachment(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@Valid @RequestBody FileAttachmentRequest request
	) {
		FileAttachmentResponse response = s3AttachmentService.saveAttachment(
			userPrincipal.getUserId(),
			request
		);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("파일 정보가 성공적으로 등록되었습니다.", response));
	}

	@DeleteMapping("/{fileId}")
	public ResponseEntity<Void> deleteAttachment(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable Long fileId
	) {
		s3AttachmentService.deleteAttachment(userPrincipal.getUserId(), fileId);

		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
