package com.ktb3.devths.auth.util;

import jakarta.servlet.http.Cookie;

public class CookieUtil {
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	private static final String COOKIE_PATH = "/api/auth";
	private static final String COOKIE_DOMAIN = ".devths.com";
	private static final int REFRESH_TOKEN_MAX_AGE = 14 * 24 * 60 * 60; // 14일 (초 단위)

	/**
	 * Refresh Token Cookie 생성
	 *
	 * @param refreshToken Refresh Token 값
	 * @return Cookie 객체
	 */
	public static Cookie createRefreshTokenCookie(String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setDomain(COOKIE_DOMAIN);
		cookie.setPath(COOKIE_PATH);
		cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
		cookie.setAttribute("SameSite", "Lax");

		return cookie;
	}

	/**
	 * Refresh Token Cookie 삭제
	 *
	 * @return Max-Age가 0인 Cookie 객체
	 */
	public static Cookie clearRefreshTokenCookie() {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setDomain(COOKIE_DOMAIN);
		cookie.setPath(COOKIE_PATH);
		cookie.setMaxAge(0);
		cookie.setAttribute("SameSite", "Lax");

		return cookie;
	}

	/**
	 * Cookie 배열에서 Refresh Token 추출
	 *
	 * @param cookies Cookie 배열
	 * @return Refresh Token 값 (없으면 null)
	 */
	public static String getRefreshTokenFromCookies(Cookie[] cookies) {
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}
}
