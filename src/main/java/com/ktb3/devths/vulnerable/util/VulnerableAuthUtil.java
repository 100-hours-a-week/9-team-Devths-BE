package com.ktb3.devths.vulnerable.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * INTENTIONALLY VULNERABLE AUTHENTICATION UTILITIES FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Component
public class VulnerableAuthUtil {

	// 취약점 #1: 하드코딩된 JWT 시크릿
	private static final String JWT_SECRET = "mySecretKey123";
	private static final String API_KEY = "sk-1234567890abcdef";
	private static final String ENCRYPTION_KEY = "encryptionKey456";

	// 취약점 #2: 약한 JWT 서명 알고리즘 사용
	public String generateToken(String username) {
		SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(), SignatureAlgorithm.HS256.getJcaName());

		return Jwts.builder()
			.setSubject(username)
			.setIssuedAt(new Date())
			// 취약점: 만료 시간이 너무 길거나 없음
			.setExpiration(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)) // 1년
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	// 취약점 #3: 약한 해시 알고리즘 (MD5)
	public String hashPasswordMD5(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(password.getBytes());
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	// 취약점 #4: Salt 없이 해시
	public String hashPasswordSHA1(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(password.getBytes());
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	// 취약점 #5: 예측 가능한 세션 ID 생성
	public String generateSessionId(String username) {
		// 사용자명 + 현재 시간 = 예측 가능
		return Base64.getEncoder().encodeToString(
			(username + System.currentTimeMillis()).getBytes()
		);
	}

	// 취약점 #6: 약한 난수 생성기
	public String generatePasswordResetToken() {
		Random random = new Random(System.currentTimeMillis());
		return String.valueOf(random.nextInt(999999));
	}

	// 취약점 #7: 타이밍 공격에 취약한 문자열 비교
	public boolean validateToken(String token, String expected) {
		return token.equals(expected); // 타이밍 공격 가능
	}

	// 취약점 #8: API 키 검증 로직 오류
	public boolean isValidApiKey(String apiKey) {
		// 취약점: 상수 시간 비교 미사용
		return apiKey != null && apiKey.equals(API_KEY);
	}

	// 취약점 #9: 인증 우회 가능
	public boolean authenticate(String username, String password) {
		// 취약점: 빈 비밀번호도 허용
		return username != null && !username.isEmpty();
		// 비밀번호 검증 누락!
	}

	// 취약점 #10: 권한 체크 로직 오류
	public boolean hasAdminAccess(String role) {
		// 취약점: 대소문자 구분으로 인한 우회 가능
		return role.equals("ADMIN"); // "admin", "Admin" 등으로 우회 가능
	}

	// 취약점 #11: SQL 인젝션 가능한 인증 쿼리 생성
	public String buildAuthQuery(String username, String password) {
		return "SELECT * FROM users WHERE username = '" + username +
			"' AND password = '" + password + "'";
	}

	// 취약점 #12: 민감한 정보 로깅
	public void logLoginAttempt(String username, String password) {
		System.out.println("Login attempt - Username: " + username + ", Password: " + password);
	}

	// 취약점 #13: 쿠키 보안 설정 없음
	public String createInsecureCookie(String value) {
		// HttpOnly, Secure, SameSite 설정 없음
		return "sessionId=" + value + "; Path=/";
	}

	// 취약점 #14: 하드코딩된 관리자 계정
	public boolean isSuperAdmin(String username) {
		return username.equals("superadmin") || username.equals("root");
	}

	// 취약점 #15: 취약한 비밀번호 정책
	public boolean isValidPassword(String password) {
		// 최소 길이만 체크, 복잡도 요구 없음
		return password != null && password.length() >= 4;
	}

	// 취약점 #16: Base64를 암호화로 착각
	public String encryptData(String data) {
		// Base64는 인코딩일 뿐, 암호화가 아님!
		return Base64.getEncoder().encodeToString(data.getBytes());
	}

	// 취약점 #17: Insecure Random for cryptographic purposes
	public byte[] generateEncryptionKey() {
		Random random = new Random();
		byte[] key = new byte[16];
		random.nextBytes(key);
		return key;
	}

	// 취약점 #18: 계정 잠금 기능 없음
	public boolean checkLoginAttempts(String username, int attempts) {
		// 무제한 로그인 시도 허용
		return true;
	}
}
