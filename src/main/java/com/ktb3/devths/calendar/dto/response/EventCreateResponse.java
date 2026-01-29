package com.ktb3.devths.calendar.dto.response;

public record EventCreateResponse(
	String eventId
) {
	public static EventCreateResponse of(String googleEventId) {
		return new EventCreateResponse(googleEventId);
	}
}
