package com.ktb3.devths.global.util;

/**
 * 로그 인젝션(Log Injection) 방어를 위한 유틸리티 클래스
 * CRLF Injection 공격을 방어하기 위해 개행 문자를 제거합니다.
 */
public final class LogSanitizer {

	private LogSanitizer() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	/**
	 * 로그에 안전하게 기록할 수 있도록 문자열을 sanitize합니다.
	 * 개행 문자(\n, \r)를 공백으로 치환하여 CRLF Injection을 방어합니다.
	 *
	 * @param input sanitize할 문자열
	 * @return sanitize된 문자열 (null 입력 시 "null" 반환)
	 */
	public static String sanitize(String input) {
		if (input == null) {
			return "null";
		}
		return input.replaceAll("[\r\n]", " ");
	}
}
