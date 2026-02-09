package com.ktb3.devths.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.board.dto.response.PostListResponse;
import com.ktb3.devths.board.service.PostService;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@GetMapping
	public ResponseEntity<ApiResponse<PostListResponse>> getPosts(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) Long lastId,
		@RequestParam(required = false) String tag
	) {
		PostListResponse response = postService.getPostList(size, lastId, tag);

		return ResponseEntity.ok(
			ApiResponse.success("게시글 목록을 성공적으로 조회하였습니다.", response)
		);
	}
}
