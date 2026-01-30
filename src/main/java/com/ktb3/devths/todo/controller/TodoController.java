package com.ktb3.devths.todo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;
import com.ktb3.devths.todo.dto.request.TodoCreateRequest;
import com.ktb3.devths.todo.dto.response.TodoCreateResponse;
import com.ktb3.devths.todo.dto.response.TodoResponse;
import com.ktb3.devths.todo.service.TodoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;

	/**
	 * To-do 추가
	 *
	 * @param userPrincipal 인증된 사용자
	 * @param request To-do 추가 요청
	 * @return To-do 추가 응답
	 */
	@PostMapping
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201")
	public ResponseEntity<ApiResponse<TodoCreateResponse>> createTodo(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@Valid @RequestBody TodoCreateRequest request
	) {
		TodoCreateResponse response = todoService.createTodo(userPrincipal.getUserId(), request);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success("할 일이 성공적으로 추가되었습니다.", response));
	}

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
