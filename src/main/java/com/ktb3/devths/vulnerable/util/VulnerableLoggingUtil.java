package com.ktb3.devths.vulnerable.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * INTENTIONALLY VULNERABLE LOGGING UTILITIES FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 *
 * Contains: Information Disclosure, Log Injection, Sensitive Data Exposure
 */
@Component
public class VulnerableLoggingUtil {

	private static final Logger logger = LoggerFactory.getLogger(VulnerableLoggingUtil.class);

	// 취약점 #1: 비밀번호 로깅
	public void logLoginAttempt(String username, String password) {
		// 취약점: 민감한 정보 (비밀번호) 로깅
		logger.info("Login attempt - Username: {}, Password: {}", username, password);
	}

	// 취약점 #2: 신용카드 정보 로깅
	public void logPayment(String cardNumber, String cvv, String amount) {
		// 취약점: 신용카드 정보 로깅
		logger.info("Payment processed - Card: {}, CVV: {}, Amount: {}", cardNumber, cvv, amount);
	}

	// 취약점 #3: 전체 요청 바디 로깅 (민감 정보 포함 가능)
	public void logRequest(Map<String, Object> requestBody) {
		// 취약점: 전체 요청 로깅 (password, token 등 포함 가능)
		logger.info("Request received: {}", requestBody);
	}

	// 취약점 #4: 스택 트레이스 전체 로깅
	public void logException(Exception e) {
		// 취약점: 스택 트레이스에서 시스템 정보 노출
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		logger.error("Exception occurred: {}", sw);
	}

	// 취약점 #5: Log Injection
	public void logUserInput(String userInput) {
		// 취약점: 사용자 입력을 검증 없이 로그에 기록
		// 예: userInput = "User logged in\nADMIN logged in" -> 로그 위조
		logger.info("User activity: " + userInput);
	}

	// 취약점 #6: API 키 로깅
	public void logApiCall(String endpoint, String apiKey) {
		// 취약점: API 키 로깅
		logger.info("API call to {} with key: {}", endpoint, apiKey);
	}

	// 취약점 #7: JWT 토큰 로깅
	public void logAuthToken(String token) {
		// 취약점: 인증 토큰 로깅
		logger.info("Authentication token: {}", token);
	}

	// 취약점 #8: 세션 ID 로깅
	public void logSession(String sessionId, String username) {
		// 취약점: 세션 ID 로깅
		logger.info("Session created - ID: {}, User: {}", sessionId, username);
	}

	// 취약점 #9: 개인 식별 정보 (PII) 로깅
	public void logUserRegistration(String username, String email, String ssn, String phoneNumber) {
		// 취약점: PII 정보 로깅
		logger.info("New user registered - Username: {}, Email: {}, SSN: {}, Phone: {}",
			username, email, ssn, phoneNumber);
	}

	// 취약점 #10: SQL 쿼리 로깅 (데이터 포함)
	public void logSqlQuery(String query) {
		// 취약점: 데이터가 포함된 SQL 쿼리 로깅
		logger.debug("Executing SQL: {}", query);
	}

	// 취약점 #11: 파일 경로 로깅 (시스템 구조 노출)
	public void logFileAccess(String fullPath) {
		// 취약점: 전체 파일 경로 로깅
		logger.info("File accessed: {}", fullPath);
	}

	// 취약점 #12: 환경 변수 로깅
	public void logEnvironment() {
		// 취약점: 환경 변수 전체 로깅 (비밀 정보 포함 가능)
		Map<String, String> env = System.getenv();
		logger.info("Environment variables: {}", env);
	}

	// 취약점 #13: 시스템 속성 로깅
	public void logSystemProperties() {
		// 취약점: 시스템 속성 로깅
		logger.info("System properties: {}", System.getProperties());
	}

	// 취약점 #14: 데이터베이스 연결 문자열 로깅
	public void logDatabaseConnection(String connectionString) {
		// 취약점: DB 연결 문자열 로깅 (비밀번호 포함)
		logger.info("Database connection: {}", connectionString);
	}

	// 취약점 #15: 디버그 정보 과다 노출
	public void logDebugInfo(Object object) {
		// 취약점: toString()으로 모든 객체 정보 로깅
		logger.debug("Debug info: {}", object.toString());
	}

	// 취약점 #16: 에러 응답에 민감한 정보 포함
	public String createErrorResponse(Exception e) {
		// 취약점: 에러 메시지에 스택 트레이스 포함
		return "Error: " + e.getMessage() + "\nStack trace: " + getStackTrace(e);
	}

	private String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	// 취약점 #17: IDOR 정보 로깅
	public void logResourceAccess(Long userId, Long resourceId) {
		// 취약점: 리소스 접근 패턴 노출
		logger.info("User {} accessed resource {}", userId, resourceId);
	}

	// 취약점 #18: 암호화 키 로깅
	public void logEncryptionKey(String key) {
		// 취약점: 암호화 키 로깅
		logger.warn("Encryption key used: {}", key);
	}

	// 취약점 #19: 내부 IP 주소 로깅
	public void logInternalNetwork(String internalIp, String serviceName) {
		// 취약점: 내부 네트워크 정보 노출
		logger.info("Connected to internal service {} at {}", serviceName, internalIp);
	}

	// 취약점 #20: 타이밍 정보 로깅 (타이밍 공격에 활용 가능)
	public void logAuthenticationTime(String username, long duration) {
		// 취약점: 인증 시간 로깅
		logger.info("Authentication for user {} took {} ms", username, duration);
	}
}
