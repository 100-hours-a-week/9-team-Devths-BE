package com.ktb3.devths.todo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;
import com.ktb3.devths.todo.dto.response.TodoResponse;
import com.ktb3.devths.todo.service.TodoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;

	/**
	 * To-do 목록 조회
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param dueDate 마감일 필터 (선택, yyyy-MM-dd)
	 * @return To-do 목록
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodos(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(required = false) String dueDate
	) {
		List<TodoResponse> response = todoService.getTodos(userPrincipal.getUserId(), dueDate);

		return ResponseEntity
			.ok(ApiResponse.success("할 일 목록 조회에 성공하였습니다.", response));
	}
}
