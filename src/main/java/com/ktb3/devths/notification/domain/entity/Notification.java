package com.ktb3.devths.notification.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ktb3.devths.notification.domain.constant.NotificationCategory;
import com.ktb3.devths.notification.domain.constant.NotificationType;
import com.ktb3.devths.user.domain.entity.User;

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
@Table(name = "notifications")
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id", nullable = true)
	private User sender;

	@Column(name = "category", nullable = false)
	private NotificationCategory category;

	@Column(name = "type", nullable = false)
	private NotificationType type;

	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "target_path", nullable = false)
	private String targetPath; //알림 클릭 시 이동할 프론트엔드 라우팅 경로(ex. /posts/123)

	@Column(name = "resource_id", nullable = false)
	private Long resourceId; //데이터 조회용 참조 데이터 ID

	@Column(name = "is_read", nullable = false)
	private boolean isRead = false;

	@Column(name = "created_at", nullable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at", nullable = true)
	private LocalDateTime deletedAt;
}
