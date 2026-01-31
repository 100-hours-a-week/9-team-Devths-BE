package com.ktb3.devths.user.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "social_accounts",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_provider_providerUserId",
			columnNames = {"provider", "provider_user_id"}
		)
	})
public class SocialAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "provider", nullable = false)
	private String provider = "GOOGLE";

	@Column(name = "provider_user_id", nullable = false)
	private String providerUserId;

	@Column(name = "created_at", nullable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "access_token", nullable = false, columnDefinition = "VARCHAR(1024)")
	private String accessToken;

	@Column(name = "refresh_token", nullable = false, columnDefinition = "VARCHAR(1024)")
	private String refreshToken;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	/**
	 * Google Access Token / Refresh Token 갱신
	 *
	 * @param encryptedAccessToken 암호화된 Google Access Token
	 * @param encryptedRefreshToken 암호화된 Google Refresh Token
	 * @param expiresAt Google Access Token 만료 시간
	 */
	public void updateTokens(String encryptedAccessToken, String encryptedRefreshToken, LocalDateTime expiresAt) {
		this.accessToken = encryptedAccessToken;
		this.refreshToken = encryptedRefreshToken;
		this.expiresAt = expiresAt;
	}
}
