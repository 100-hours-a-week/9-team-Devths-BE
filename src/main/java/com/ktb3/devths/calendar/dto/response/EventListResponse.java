package com.ktb3.devths.calendar.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;

public record EventListResponse(
	String eventId,
	String title,
	LocalDateTime startTime,
	LocalDateTime endTime,
	InterviewStage stage,
	List<String> tags
) {
	public static EventListResponse of(
		String eventId,
		String title,
		LocalDateTime startTime,
		LocalDateTime endTime,
		InterviewStage stage,
		List<String> tags
	) {
		return new EventListResponse(eventId, title, startTime, endTime, stage, tags);
	}
}
