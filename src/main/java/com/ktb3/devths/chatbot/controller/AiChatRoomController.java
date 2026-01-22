package com.ktb3.devths.chatbot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.chatbot.dto.response.AiChatRoomCreateResponse;
import com.ktb3.devths.chatbot.dto.response.AiChatRoomListResponse;
import com.ktb3.devths.chatbot.service.AiChatRoomService;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai-chatrooms")
@RequiredArgsConstructor
public class AiChatRoomController {

	private final AiChatRoomService aiChatRoomService;

	@PostMapping
	public ResponseEntity<ApiResponse<AiChatRoomCreateResponse>> createChatRoom(
		@AuthenticationPrincipal UserPrincipal userPrincipal
	) {
		AiChatRoomCreateResponse response = aiChatRoomService.createChatRoom(userPrincipal.getUserId());

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("AI 채팅방이 성공적으로 생성되었습니다.", response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<AiChatRoomListResponse>> getChatRoomList(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) Long lastId
	) {
		AiChatRoomListResponse response = aiChatRoomService.getChatRoomList(
			userPrincipal.getUserId(),
			size,
			lastId
		);

		return ResponseEntity.ok(
			ApiResponse.success("AI 채팅방 목록을 성공적으로 조회하였습니다.", response)
		);
	}
}
