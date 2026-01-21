package com.ktb3.devths.global.storage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.global.storage.domain.S3Attachment;
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
}
