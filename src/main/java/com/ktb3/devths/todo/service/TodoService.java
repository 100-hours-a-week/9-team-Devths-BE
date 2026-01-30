package com.ktb3.devths.todo.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.services.tasks.model.Task;
import com.ktb3.devths.todo.dto.request.TodoCreateRequest;
import com.ktb3.devths.todo.dto.response.TodoCreateResponse;
import com.ktb3.devths.todo.dto.response.TodoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

	private static final String TIMEZONE = "Asia/Seoul";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final GoogleTasksService googleTasksService;

	/**
	 * To-do 목록 조회
	 *
	 * @param userId 사용자 ID
	 * @param dueDate 마감일 필터 (yyyy-MM-dd, 선택)
	 * @return To-do 목록
	 */
	@Transactional(readOnly = true)
	public List<TodoResponse> getTodos(Long userId, String dueDate) {
		// 1. Google Tasks API 호출
		List<Task> tasks = googleTasksService.listTasks(userId);

		// 2. dueDate 필터링 및 DTO 변환
		return tasks.stream()
			.filter(task -> matchesDueDateFilter(task, dueDate))
			.map(this::convertToTodoResponse)
			.toList();
	}

	/**
	 * To-do 추가
	 *
	 * @param userId 사용자 ID
	 * @param request To-do 추가 요청
	 * @return To-do 추가 응답
	 */
	@Transactional
	public TodoCreateResponse createTodo(Long userId, TodoCreateRequest request) {
		String taskId = googleTasksService.createTask(userId, request.title(), request.dueDate());
		return new TodoCreateResponse(taskId);
	}

	/**
	 * dueDate 필터 매칭
	 */
	private boolean matchesDueDateFilter(Task task, String dueDate) {
		if (dueDate == null) {
			return true;
		}

		if (task.getDue() == null) {
			return false;
		}

		try {
			LocalDate taskDueDate = convertToLocalDate(task.getDue());
			LocalDate filterDate = LocalDate.parse(dueDate, DATE_FORMATTER);
			return taskDueDate.equals(filterDate);
		} catch (Exception e) {
			log.warn("날짜 필터링 실패: taskId={}, dueDate={}", task.getId(), dueDate, e);
			return false;
		}
	}

	/**
	 * Google Task를 TodoResponse로 변환
	 */
	private TodoResponse convertToTodoResponse(Task task) {
		String todoId = task.getId();
		String title = task.getTitle();
		boolean isCompleted = "completed".equals(task.getStatus());
		String dueDate = task.getDue() != null ? convertToLocalDate(task.getDue()).format(DATE_FORMATTER) : null;

		return new TodoResponse(todoId, title, isCompleted, dueDate);
	}

	/**
	 * Google Tasks DateTime (RFC 3339)을 LocalDate로 변환
	 */
	private LocalDate convertToLocalDate(String dueDateString) {
		// RFC 3339 형식의 문자열을 파싱 (예: "2026-01-30T00:00:00.000Z")
		Instant instant = Instant.parse(dueDateString);
		return LocalDate.ofInstant(instant, ZoneId.of(TIMEZONE));
	}
}
