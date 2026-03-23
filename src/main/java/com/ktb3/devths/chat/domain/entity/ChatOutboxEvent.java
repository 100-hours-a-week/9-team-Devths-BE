package com.ktb3.devths.chat.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ktb3.devths.chat.domain.constant.ChatOutboxEventStatus;
import com.ktb3.devths.chat.domain.constant.ChatOutboxEventType;

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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_outbox_events")
public class ChatOutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private ChatOutboxEventType eventType;

	@Column(name = "aggregate_type", nullable = false)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private Long aggregateId;

	@Column(name = "payload", nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Column(name = "trace_context", columnDefinition = "TEXT")
	private String traceContext;

	@Builder.Default
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ChatOutboxEventStatus status = ChatOutboxEventStatus.PENDING;

	@Builder.Default
	@Column(name = "retry_count", nullable = false)
	private int retryCount = 0;

	@Column(name = "created_at", nullable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "next_retry_at")
	private LocalDateTime nextRetryAt;

	public void markPublished() {
		this.status = ChatOutboxEventStatus.PUBLISHED;
		this.publishedAt = LocalDateTime.now();
	}

	public void markFailed() {
		this.status = ChatOutboxEventStatus.FAILED;
	}

	public void incrementRetryCount() {
		this.retryCount++;
	}
}
