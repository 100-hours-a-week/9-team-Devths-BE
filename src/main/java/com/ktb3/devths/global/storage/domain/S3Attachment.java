package com.ktb3.devths.global.storage.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@Table(name = "s3_attachments")
public class S3Attachment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "original_name", nullable = false)
	private String originalName;

	@Column(name = "ref_type", nullable = false)
	private String refType;

	@Column(name = "ref_id", nullable = true)
	private Long refId;

	@Column(name = "s3_key", nullable = true)
	private String s3Key;

	@Column(name = "mime_type", nullable = false)
	private String mimeType;

	@Column(name = "category", nullable = true)
	private String category;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;

	@Column(name = "created_at", nullable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at", nullable = true)
	private LocalDateTime deletedAt;
}
