package com.ktb3.devths.global.storage.dto.response;

import java.time.LocalDateTime;

public record FileAttachmentResponse(
	Long fileId,
	String s3Key,
	LocalDateTime createdAt
) {
}
