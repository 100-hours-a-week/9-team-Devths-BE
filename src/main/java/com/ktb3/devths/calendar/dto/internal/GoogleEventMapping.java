package com.ktb3.devths.calendar.dto.internal;

import java.time.LocalDateTime;
import java.util.List;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;

import lombok.Builder;

@Builder
public record GoogleEventMapping(
	String summary,
	String company,
	String description,
	LocalDateTime startTime,
	LocalDateTime endTime,
	InterviewStage stage,
	List<String> tags,
	int notificationMinutes
) {
}
