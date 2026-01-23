package com.ktb3.devths.global.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ktb3.devths.global.security.UserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtTokenValidator jwtTokenValidator;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		try {
			String token = extractTokenFromRequest(request);

			if (token != null) {
				jwtTokenValidator.validateAccessToken(token);

				Long userId = jwtTokenProvider.getUserIdFromToken(token);
				String email = jwtTokenProvider.getEmailFromToken(token);
				String role = jwtTokenProvider.getRoleFromToken(token);

				UserPrincipal userPrincipal = new UserPrincipal(userId, email, role);

				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userPrincipal, null,
						userPrincipal.getAuthorities());

				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);

				log.debug("JWT 검증 성공 - userId: {}", userId);
			}
		} catch (Exception e) {
			log.warn("JWT 인증 실패");
		}

		filterChain.doFilter(request, response);
	}

	private String extractTokenFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}

		return null;
	}
}
