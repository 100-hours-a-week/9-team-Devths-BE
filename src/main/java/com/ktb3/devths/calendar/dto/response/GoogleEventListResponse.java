package com.ktb3.devths.calendar.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;

public record GoogleEventListResponse(
	String eventId,
	String title,
	LocalDateTime startTime,
	LocalDateTime endTime,
	InterviewStage stage,
	List<String> tags
) {
	public static GoogleEventListResponse of(
		String eventId,
		String title,
		LocalDateTime startTime,
		LocalDateTime endTime,
		InterviewStage stage,
		List<String> tags
	) {
		return new GoogleEventListResponse(eventId, title, startTime, endTime, stage, tags);
	}
}
