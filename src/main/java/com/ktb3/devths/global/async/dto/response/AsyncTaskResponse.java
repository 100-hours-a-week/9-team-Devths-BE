package com.ktb3.devths.global.async.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record AsyncTaskResponse(
	Long taskId,
	String taskType,
	Long referenceId,
	String status,
	Map<String, Object> result,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	boolean isNotified
) {
}
