package com.ktb3.devths.global.storage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
	@NotBlank(message = "파일 이름은 필수입니다.")
	String fileName,

	@NotBlank(message = "MIME 타입은 필수입니다.")
	String mimeType
) {
}
