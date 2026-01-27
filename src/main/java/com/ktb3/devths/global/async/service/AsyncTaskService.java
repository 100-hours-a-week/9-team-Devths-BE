package com.ktb3.devths.global.async.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.global.async.domain.constant.TaskStatus;
import com.ktb3.devths.global.async.domain.constant.TaskType;
import com.ktb3.devths.global.async.domain.entity.AsyncTask;
import com.ktb3.devths.global.async.repository.AsyncTaskRepository;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.user.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncTaskService {

	private final AsyncTaskRepository asyncTaskRepository;

	@Transactional
	public AsyncTask createTask(User user, TaskType taskType, Long referenceId) {
		AsyncTask task = AsyncTask.builder()
			.user(user)
			.taskType(taskType)
			.referenceId(referenceId)
			.status(TaskStatus.PENDING)
			.build();

		return asyncTaskRepository.save(task);
	}

	@Transactional
	public void updateStatus(Long taskId, TaskStatus status) {
		AsyncTask task = asyncTaskRepository.findById(taskId)
			.orElseThrow(() -> new CustomException(ErrorCode.ASYNC_TASK_NOT_FOUND));

		task.updateStatus(status);
	}

	@Transactional
	public void updateResult(Long taskId, Map<String, Object> result) {
		AsyncTask task = asyncTaskRepository.findById(taskId)
			.orElseThrow(() -> new CustomException(ErrorCode.ASYNC_TASK_NOT_FOUND));

		task.updateResult(result);
		task.updateStatus(TaskStatus.COMPLETED);
	}

	@Transactional
	public void markAsFailed(Long taskId, String reason) {
		AsyncTask task = asyncTaskRepository.findById(taskId)
			.orElseThrow(() -> new CustomException(ErrorCode.ASYNC_TASK_NOT_FOUND));

		task.markAsFailed(reason);
	}

	@Transactional
	public void setExternalTaskId(Long taskId, String externalTaskId) {
		AsyncTask task = asyncTaskRepository.findById(taskId)
			.orElseThrow(() -> new CustomException(ErrorCode.ASYNC_TASK_NOT_FOUND));

		task.setExternalTaskId(externalTaskId);
	}
}
