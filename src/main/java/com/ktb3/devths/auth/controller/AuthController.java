package com.ktb3.devths.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.auth.dto.internal.GoogleLoginResult;
import com.ktb3.devths.auth.dto.request.GoogleLoginRequest;
import com.ktb3.devths.auth.dto.response.GoogleLoginResponse;
import com.ktb3.devths.auth.service.AuthService;
import com.ktb3.devths.auth.util.CookieUtil;
import com.ktb3.devths.global.response.ApiResponse;
import com.ktb3.devths.global.security.UserPrincipal;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	/**
	 * Google OAuth2 로그인
	 */
	@PostMapping("/google")
	public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(
		@Valid @RequestBody GoogleLoginRequest request,
		HttpServletResponse response
	) {
		GoogleLoginResult result = authService.loginWithGoogle(request.authCode());

		if (result.isRegistered()) {
			// Access Token을 Authorization 헤더에 설정
			response.setHeader("Authorization", "Bearer " + result.tokenPair().accessToken());

			// Refresh Token을 HttpOnly Cookie로 설정
			response.addCookie(CookieUtil.createRefreshTokenCookie(result.tokenPair().refreshToken()));
		}

		String message = result.isRegistered()
			? "로그인에 성공하였습니다."
			: "신규 유저입니다. 회원가입이 필요합니다.";

		GoogleLoginResponse loginResponse = result.isRegistered()
			? GoogleLoginResponse.registered(result.loginResponse())
			: GoogleLoginResponse.newUser(result.email(), result.tempToken());

		return ResponseEntity.ok()
			.header("Access-Control-Expose-Headers", "Authorization")
			.header("Access-Control-Allow-Credentials", "true")
			.body(ApiResponse.success(message, loginResponse));
	}

	/**
	 * 로그아웃
	 */
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		HttpServletResponse response
	) {
		authService.logout(userPrincipal.getUserId());

		// Refresh Token Cookie 삭제
		response.addCookie(CookieUtil.clearRefreshTokenCookie());

		return ResponseEntity.ok(ApiResponse.success("로그아웃에 성공하였습니다.", null));
	}
}
