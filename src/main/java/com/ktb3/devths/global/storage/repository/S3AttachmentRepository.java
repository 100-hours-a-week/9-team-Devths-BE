package com.ktb3.devths.global.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.global.storage.domain.S3Attachment;

public interface S3AttachmentRepository extends JpaRepository<S3Attachment, Long> {
}
