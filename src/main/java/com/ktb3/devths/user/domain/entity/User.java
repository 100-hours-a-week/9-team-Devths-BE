package com.ktb3.devths.user.domain.entity;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ktb3.devths.user.domain.UserRoles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", unique = true, nullable = false)
	private String email;

	@Column(name = "nickname", unique = true, nullable = false)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private UserRoles role = UserRoles.ROLE_USER;

	@Column(name = "created_at", nullable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Column(name = "is_withdraw", nullable = false)
	private boolean isWithdraw = false;

	@Column(name = "withdraw_at", nullable = true)
	private LocalDateTime withdrawAt;

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void withdraw() {
		this.isWithdraw = true;
		this.withdrawAt = LocalDateTime.now();
		String randomSuffix = generateRandomString(6);
		this.email = this.email + "_deleted_" + randomSuffix;
		this.nickname = this.nickname + "_deleted_" + randomSuffix;
	}

	private String generateRandomString(int length) {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(characters.charAt(random.nextInt(characters.length())));
		}
		return sb.toString();
	}
}
