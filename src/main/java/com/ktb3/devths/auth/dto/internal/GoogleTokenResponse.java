package com.ktb3.devths.auth.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("refresh_token") String refreshToken,
	@JsonProperty("id_token") String idToken,
	@JsonProperty("expires_in") int expiresIn
) {
}
