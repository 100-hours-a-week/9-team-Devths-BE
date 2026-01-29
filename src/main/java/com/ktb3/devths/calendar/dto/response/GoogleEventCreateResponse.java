package com.ktb3.devths.calendar.dto.response;

public record GoogleEventCreateResponse(
	String eventId
) {
	public static GoogleEventCreateResponse of(String googleEventId) {
		return new GoogleEventCreateResponse(googleEventId);
	}
}
