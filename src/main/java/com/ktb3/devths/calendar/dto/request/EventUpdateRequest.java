package com.ktb3.devths.calendar.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.ktb3.devths.calendar.domain.constant.InterviewStage;
import com.ktb3.devths.calendar.domain.constant.NotificationUnit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventUpdateRequest(
	@NotNull(message = "stage는 필수입니다")
	InterviewStage stage,

	@NotBlank(message = "title은 필수입니다")
	String title,

	@NotBlank(message = "company는 필수입니다")
	String company,

	@NotNull(message = "startTime은 필수입니다")
	LocalDateTime startTime,

	@NotNull(message = "endTime은 필수입니다")
	LocalDateTime endTime,

	String description,

	List<String> tags,

	@NotNull(message = "notificationTime은 필수입니다")
	@Min(value = 1, message = "notificationTime은 1 이상이어야 합니다")
	Integer notificationTime,

	@NotNull(message = "notificationUnit은 필수입니다")
	NotificationUnit notificationUnit
) {
	public EventUpdateRequest {
		if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("endTime은 startTime보다 이후여야 합니다");
		}
	}
}
