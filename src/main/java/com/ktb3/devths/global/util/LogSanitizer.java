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

	/**
	 * URI를 로그에 안전하게 기록할 수 있도록 sanitize합니다.
	 * 쿼리 스트링을 제거하고 path만 추출한 후, 개행 문자를 제거합니다.
	 *
	 * @param uri sanitize할 URI 문자열
	 * @return sanitize된 URI path (null 입력 시 "null" 반환)
	 */
	public static String sanitizeUri(String uri) {
		if (uri == null) {
			return "null";
		}
		// 쿼리 스트링 제거 (? 이전까지만 추출)
		int queryIndex = uri.indexOf('?');
		String path = queryIndex != -1 ? uri.substring(0, queryIndex) : uri;
		// CRLF 제거
		return path.replaceAll("[\r\n]", " ");
	}
}
