package com.ktb3.devths.global.storage.dto.response;

public record PresignedUrlResponse(
	String presignedUrl,
	String s3Key
) {
}
