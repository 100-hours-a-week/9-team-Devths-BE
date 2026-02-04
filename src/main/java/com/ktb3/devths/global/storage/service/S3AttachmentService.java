package com.ktb3.devths.global.storage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.storage.domain.entity.S3Attachment;
import com.ktb3.devths.global.storage.dto.request.FileAttachmentRequest;
import com.ktb3.devths.global.storage.dto.response.FileAttachmentResponse;
import com.ktb3.devths.global.storage.repository.S3AttachmentRepository;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3AttachmentService {
	private final S3AttachmentRepository s3AttachmentRepository;
	private final UserRepository userRepository;
	private final S3StorageService s3StorageService;

	@Transactional
	public FileAttachmentResponse saveAttachment(Long userId, FileAttachmentRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		S3Attachment attachment = S3Attachment.builder()
			.user(user)
			.originalName(request.originalName())
			.s3Key(request.s3Key())
			.mimeType(request.mimeType())
			.category(request.category())
			.fileSize(request.fileSize())
			.refType(request.refType())
			.refId(request.refId())
			.sortOrder(request.sortOrder())
			.build();

		S3Attachment saved = s3AttachmentRepository.save(attachment);

		return new FileAttachmentResponse(
			saved.getId(),
			saved.getS3Key(),
			saved.getCreatedAt()
		);
	}

	@Transactional
	public void deleteAttachment(Long userId, Long fileId) {
		S3Attachment attachment = s3AttachmentRepository.findById(fileId)
			.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (!attachment.getUser().getId().equals(userId)) {
			throw new CustomException(ErrorCode.ACCESS_DENIED);
		}

		s3StorageService.deleteFile(attachment.getS3Key());
		attachment.softDelete();
	}
}
