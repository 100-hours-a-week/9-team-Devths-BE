package com.ktb3.devths.global.async.domain.entity;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ktb3.devths.global.async.domain.constant.TaskStatus;
import com.ktb3.devths.global.async.domain.constant.TaskType;
import com.ktb3.devths.user.domain.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "async_tasks")
public class AsyncTask {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "task_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private TaskType taskType;

	@Column(name = "reference_id", nullable = false)
	private Long referenceId;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private TaskStatus status;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "result", nullable = true, columnDefinition = "jsonb")
	private Map<String, Object> result;

	@Column(name = "created_at", nullable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Column(name = "is_notified", nullable = true)
	private boolean isNotified = false;

	@Column(name = "external_task_id", nullable = true)
	private String externalTaskId;

	@Column(name = "fail_reason", nullable = true)
	private String failReason;

	public void updateStatus(TaskStatus status) {
		this.status = status;
	}

	public void updateResult(Map<String, Object> result) {
		this.result = result;
	}

	public void markAsFailed(String reason) {
		this.status = TaskStatus.FAILED;
		this.failReason = reason;
	}

	public void setExternalTaskId(String externalTaskId) {
		this.externalTaskId = externalTaskId;
	}
}
