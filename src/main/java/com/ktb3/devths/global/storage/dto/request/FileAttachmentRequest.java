package com.ktb3.devths.global.storage.dto.request;

import com.ktb3.devths.global.storage.domain.constant.FileCategory;
import com.ktb3.devths.global.storage.domain.constant.RefType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileAttachmentRequest(
	@NotBlank(message = "원본 파일 이름은 필수입니다.")
	String originalName,

	@NotBlank(message = "S3 Key는 필수입니다.")
	String s3Key,

	@NotBlank(message = "MIME 타입은 필수입니다.")
	String mimeType,

	FileCategory category,

	@NotNull(message = "파일 크기는 필수입니다.")
	Long fileSize,

	@NotNull(message = "참조 타입은 필수입니다.")
	RefType refType,

	Long refId,

	@NotNull(message = "정렬 순서는 필수입니다.")
	Integer sortOrder
) {
}
