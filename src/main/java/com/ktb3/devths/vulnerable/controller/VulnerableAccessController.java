package com.ktb3.devths.vulnerable.controller;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.vulnerable.service.VulnerableAccessService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * INTENTIONALLY VULNERABLE ACCESS CONTROL CODE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 *
 * Contains: IDOR, Broken Access Control, Open Redirect, etc.
 */
@RestController
@RequestMapping("/api/vulnerable/access")
@RequiredArgsConstructor
public class VulnerableAccessController {

	private final VulnerableAccessService accessService;

	// 취약점 #1: IDOR (Insecure Direct Object Reference) - 사용자 정보 조회
	@GetMapping("/user/{userId}")
	public Map<String, Object> getUserInfo(@PathVariable Long userId) {
		// 취약점: 현재 사용자가 해당 userId에 접근할 권한이 있는지 확인하지 않음
		return accessService.getUserInfo(userId);
	}

	// 취약점 #2: IDOR - 계좌 정보 조회
	@GetMapping("/account/{accountId}")
	public Map<String, Object> getAccountInfo(@PathVariable String accountId) {
		// 취약점: 권한 체크 없이 모든 계좌 정보 조회 가능
		return accessService.getAccountInfo(accountId);
	}

	// 취약점 #3: IDOR - 문서 삭제
	@DeleteMapping("/document/{documentId}")
	public String deleteDocument(@PathVariable Long documentId) {
		// 취약점: 문서 소유자 확인 없이 삭제
		return accessService.deleteDocument(documentId);
	}

	// 취약점 #4: Open Redirect - 리다이렉트 URL 검증 없음
	@GetMapping("/redirect")
	public void redirect(@RequestParam String url, HttpServletResponse response) throws IOException {
		// 취약점: 악의적인 URL로 리다이렉트 가능
		response.sendRedirect(url);
	}

	// 취약점 #5: Open Redirect - Location 헤더
	@GetMapping("/goto")
	public ResponseEntity<Void> gotoUrl(@RequestParam String target) {
		// 취약점: 외부 URL로 리다이렉트 가능
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(target));
		return new ResponseEntity<>(headers, HttpStatus.FOUND);
	}

	// 취약점 #6: Missing Function Level Access Control
	@GetMapping("/admin/users")
	public Map<String, Object> getAllUsers() {
		// 취약점: 관리자 권한 체크 없음
		return accessService.getAllUsers();
	}

	// 취약점 #7: Missing Function Level Access Control - 관리자 기능
	@PostMapping("/admin/promote/{userId}")
	public String promoteToAdmin(@PathVariable Long userId) {
		// 취약점: 권한 체크 없이 사용자를 관리자로 승격
		return accessService.promoteUserToAdmin(userId);
	}

	// 취약점 #8: Horizontal Privilege Escalation
	@PutMapping("/profile/{userId}")
	public String updateProfile(@PathVariable Long userId, @RequestBody Map<String, Object> profile) {
		// 취약점: 다른 사용자의 프로필 수정 가능
		return accessService.updateUserProfile(userId, profile);
	}

	// 취약점 #9: Mass Assignment
	@PostMapping("/user/register")
	public String registerUser(@RequestBody Map<String, Object> userData) {
		// 취약점: 모든 필드를 그대로 받아서 저장 (isAdmin, role 등도 설정 가능)
		return accessService.registerUser(userData);
	}

	// 취약점 #10: Predictable Resource Location
	@GetMapping("/invoice/{invoiceNumber}")
	public Map<String, Object> getInvoice(@PathVariable int invoiceNumber) {
		// 취약점: 순차적인 번호로 다른 사용자의 청구서 조회 가능
		return accessService.getInvoice(invoiceNumber);
	}

	// 취약점 #11: 파일 접근 권한 체크 없음
	@GetMapping("/files/{fileId}")
	public ResponseEntity<String> downloadFile(@PathVariable String fileId) {
		// 취약점: 파일 소유자 확인 없이 다운로드
		String content = accessService.getFileContent(fileId);
		return ResponseEntity.ok(content);
	}

	// 취약점 #12: API 키 노출
	@GetMapping("/api-key/{userId}")
	public Map<String, String> getApiKey(@PathVariable Long userId) {
		// 취약점: 다른 사용자의 API 키 조회 가능
		return accessService.getUserApiKey(userId);
	}

	// 취약점 #13: 세션 고정 (Session Fixation)
	@PostMapping("/login-fixed-session")
	public String loginWithFixedSession(@RequestParam String username, @RequestParam String password) {
		// 취약점: 로그인 후 세션 ID를 재생성하지 않음
		return accessService.loginWithoutSessionRegeneration(username, password);
	}

	// 취약점 #14: 불충분한 인증
	@GetMapping("/reset-password")
	public String resetPassword(@RequestParam String email, @RequestParam String newPassword) {
		// 취약점: 토큰 검증 없이 비밀번호 재설정
		return accessService.resetPasswordNoVerification(email, newPassword);
	}

	// 취약점 #15: 열거 공격 (Enumeration)
	@GetMapping("/check-username")
	public Map<String, Boolean> checkUsername(@RequestParam String username) {
		// 취약점: 사용자명 존재 여부 노출
		boolean exists = accessService.usernameExists(username);
		return Map.of("exists", exists);
	}

	// 취약점 #16: 정보 노출 - 에러 메시지
	@PostMapping("/login-verbose")
	public ResponseEntity<String> loginVerbose(@RequestParam String username, @RequestParam String password) {
		boolean userExists = accessService.usernameExists(username);
		if (!userExists) {
			// 취약점: 사용자 존재 여부 노출
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
		}

		boolean validPassword = accessService.checkPassword(username, password);
		if (!validPassword) {
			// 취약점: 비밀번호가 틀렸다는 것을 명시적으로 알려줌
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
		}

		return ResponseEntity.ok("Login successful");
	}

	// 취약점 #17: Race Condition in Access Control
	@PostMapping("/purchase/{itemId}")
	public String purchaseItem(@PathVariable Long itemId, @RequestParam Long userId) {
		// 취약점: 잔액 확인과 구매 사이에 race condition 존재
		return accessService.purchaseItemWithRaceCondition(userId, itemId);
	}

	// 취약점 #18: JWT 토큰 검증 누락
	@GetMapping("/protected-resource")
	public Map<String, Object> getProtectedResource(@RequestHeader(value = "Authorization", required = false) String token) {
		// 취약점: 토큰이 있기만 하면 검증 없이 리소스 제공
		if (token != null && !token.isEmpty()) {
			return accessService.getProtectedData();
		}
		return Map.of("error", "No token provided");
	}

	// 취약점 #19: CORS Misconfiguration
	@CrossOrigin(origins = "*", allowCredentials = "true")
	@GetMapping("/sensitive-data")
	public Map<String, Object> getSensitiveData() {
		// 취약점: 모든 origin에서 자격증명과 함께 접근 가능
		return accessService.getSensitiveData();
	}

	// 취약점 #20: Insufficient Rate Limiting
	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam String phoneNumber) {
		// 취약점: Rate limiting 없이 OTP 무제한 발송 가능
		return accessService.sendOtpNoRateLimit(phoneNumber);
	}
}
