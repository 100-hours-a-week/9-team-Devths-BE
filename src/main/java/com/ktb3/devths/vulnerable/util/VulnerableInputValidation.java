package com.ktb3.devths.vulnerable.util;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * INTENTIONALLY VULNERABLE INPUT VALIDATION FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 *
 * Contains: ReDoS, Insufficient Validation, Injection vulnerabilities
 */
@Component
public class VulnerableInputValidation {

	// 취약점 #1: ReDoS (Regular Expression Denial of Service)
	public boolean validateEmailReDoS(String email) {
		// 취약점: 복잡한 정규식으로 백트래킹 폭발
		String regex = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";
		// 더 위험한 예: "(a+)+" 같은 패턴
		return email.matches("(a+)+b");
	}

	// 취약점 #2: ReDoS - 복잡한 패턴
	public boolean validateUrlReDoS(String url) {
		// 취약점: 재귀적 정규식
		String regex = "^(https?://)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([/\\w \\.-]*)*/?$";
		return Pattern.compile(regex).matcher(url).matches();
	}

	// 취약점 #3: 입력 길이 검증 없음
	public boolean validateUsernameNoLength(String username) {
		// 취약점: 길이 제한 없음 (DoS 가능)
		return username != null && !username.isEmpty();
	}

	// 취약점 #4: 특수문자 검증 불충분
	public boolean validatePasswordWeak(String password) {
		// 취약점: 최소 길이만 체크
		return password != null && password.length() >= 4;
	}

	// 취약점 #5: SQL 인젝션 방어 미흡
	public String sanitizeSqlInputBad(String input) {
		// 취약점: 블랙리스트 방식 (우회 가능)
		return input.replace("'", "").replace(";", "");
	}

	// 취약점 #6: XSS 방어 미흡
	public String sanitizeHtmlBad(String input) {
		// 취약점: 일부 태그만 제거
		return input.replace("<script>", "").replace("</script>", "");
	}

	// 취약점 #7: 경로 검증 미흡
	public boolean isValidPathBad(String path) {
		// 취약점: ../ 만 체크 (우회 가능)
		return !path.contains("../");
	}

	// 취약점 #8: 이메일 검증 미흡
	public boolean validateEmailWeak(String email) {
		// 취약점: @ 포함 여부만 체크
		return email != null && email.contains("@");
	}

	// 취약점 #9: URL 검증 미흡
	public boolean validateUrlWeak(String url) {
		// 취약점: http로 시작하는지만 체크
		return url != null && url.startsWith("http");
	}

	// 취약점 #10: 파일 확장자 검증 우회 가능
	public boolean isAllowedFileExtension(String filename) {
		// 취약점: 대소문자 구분, null byte 미처리
		return filename.endsWith(".jpg") || filename.endsWith(".png");
	}

	// 취약점 #11: Integer Overflow 체크 없음
	public int addWithoutOverflowCheck(int a, int b) {
		// 취약점: Integer overflow 체크 없음
		return a + b;
	}

	// 취약점 #12: Null 검증 누락
	public int getStringLengthNoNullCheck(String str) {
		// 취약점: NPE 발생 가능
		return str.length();
	}

	// 취약점 #13: Array Index 검증 없음
	public String getArrayElementUnsafe(String[] array, int index) {
		// 취약점: ArrayIndexOutOfBoundsException 가능
		return array[index];
	}

	// 취약점 #14: 타입 검증 없음
	@SuppressWarnings("unchecked")
	public void processJsonUnsafe(Object json) {
		// 취약점: 타입 검증 없이 캐스팅
		java.util.Map<String, Object> data = (java.util.Map<String, Object>)json;
	}

	// 취약점 #15: 범위 검증 없음
	public void setAgeNoValidation(int age) {
		// 취약점: 음수나 비현실적인 값 허용
		// age가 -100이나 10000이어도 OK
	}

	// 취약점 #16: 화이트리스트 없는 Enum 검증
	public void processUserRole(String role) {
		// 취약점: 임의의 문자열 허용
		// "SUPER_ADMIN", "GOD" 등도 허용됨
	}

	// 취약점 #17: CRLF Injection
	public String buildHttpHeaderUnsafe(String userInput) {
		// 취약점: \r\n 검증 없음
		return "Custom-Header: " + userInput;
	}

	// 취약점 #18: 숫자 문자열 검증 없음
	public int parseIntUnsafe(String input) {
		// 취약점: NumberFormatException 처리 없음
		return Integer.parseInt(input);
	}

	// 취약점 #19: 날짜 형식 검증 없음
	public void processDateUnsafe(String dateStr) {
		// 취약점: 형식 검증 없이 사용
		// dateStr이 "not-a-date"여도 처리 시도
	}

	// 취약점 #20: 재귀 깊이 제한 없음
	public int recursiveCalculation(int n) {
		// 취약점: 스택 오버플로우 가능
		if (n <= 0)
			return 0;
		return n + recursiveCalculation(n - 1);
	}

	// 추가 취약점: Unicode 정규화 미처리
	public boolean validateUsernameUnicode(String username) {
		// 취약점: Unicode 정규화 없이 비교
		return username.equals("admin");
	}

	// 추가 취약점: Command Injection 검증 미흡
	public boolean isSafeCommand(String command) {
		// 취약점: 일부 문자만 블랙리스트
		return !command.contains(";") && !command.contains("&");
	}
}
