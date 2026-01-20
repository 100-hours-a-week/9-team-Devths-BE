package com.ktb3.devths.auth.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleIdTokenPayload(
	String sub,
	String email,
	@JsonProperty("email_verified") boolean emailVerified
) {
}
