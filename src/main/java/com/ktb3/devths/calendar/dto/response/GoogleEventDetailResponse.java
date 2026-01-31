package com.ktb3.devths.calendar.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;
import com.ktb3.devths.calendar.domain.constant.NotificationUnit;

public record GoogleEventDetailResponse(
	String eventId,
	InterviewStage stage,
	String title,
	String company,
	LocalDateTime startTime,
	LocalDateTime endTime,
	String description,
	Integer notificationTime,
	NotificationUnit notificationUnit,
	List<String> tags,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static GoogleEventDetailResponse of(
		String eventId,
		InterviewStage stage,
		String title,
		String company,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String description,
		Integer notificationTime,
		NotificationUnit notificationUnit,
		List<String> tags,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {
		return new GoogleEventDetailResponse(
			eventId, stage, title, company, startTime, endTime,
			description, notificationTime, notificationUnit, tags,
			createdAt, updatedAt
		);
	}
}
