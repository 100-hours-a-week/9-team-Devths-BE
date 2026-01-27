package com.ktb3.devths.global.async.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.global.async.domain.entity.AsyncTask;
import com.ktb3.devths.global.async.dto.response.AsyncTaskResponse;
import com.ktb3.devths.global.async.repository.AsyncTaskRepository;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncTaskQueryService {

	private final AsyncTaskRepository asyncTaskRepository;

	@Transactional(readOnly = true)
	public AsyncTaskResponse getTaskStatus(Long userId, Long taskId) {
		AsyncTask task = asyncTaskRepository.findByIdAndUserId(taskId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.ASYNC_TASK_NOT_FOUND));

		return new AsyncTaskResponse(
			task.getId(),
			task.getTaskType().name(),
			task.getReferenceId(),
			task.getStatus().name(),
			task.getResult(),
			task.getCreatedAt(),
			task.getUpdatedAt(),
			task.isNotified()
		);
	}
}
