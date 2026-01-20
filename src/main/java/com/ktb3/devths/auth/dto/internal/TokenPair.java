package com.ktb3.devths.auth.dto.internal;

import java.time.LocalDateTime;

public record TokenPair(
	String accessToken,
	String refreshToken,
	LocalDateTime refreshTokenExpiresAt
) {
}
