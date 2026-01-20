package com.ktb3.devths.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.auth.util.CookieUtil;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.user.dto.internal.UserSignupResult;
import com.ktb3.devths.user.dto.request.UserSignupRequest;
import com.ktb3.devths.user.dto.response.UserSignupResponse;
import com.ktb3.devths.user.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@PostMapping
	public ResponseEntity<ApiResponse<UserSignupResponse>> signup(
		@Valid @RequestBody UserSignupRequest request,
		HttpServletResponse response
	) {
		UserSignupResult result = userService.signup(request);

		response.setHeader("Authorization", "Bearer " + result.tokenPair().accessToken());
		response.addCookie(CookieUtil.createRefreshTokenCookie(result.tokenPair().refreshToken()));

		return ResponseEntity.status(HttpStatus.CREATED)
			.header("Access-Control-Expose-Headers", "Authorization")
			.header("Access-Control-Allow-Credentials", "true")
			.body(ApiResponse.success("회원가입에 성공하였습니다.", result.response()));
	}
}
