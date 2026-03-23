package com.ktb3.devths.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.chat.domain.constant.ChatOutboxEventStatus;
import com.ktb3.devths.chat.domain.entity.ChatOutboxEvent;

public interface ChatOutboxEventRepository extends JpaRepository<ChatOutboxEvent, Long> {

	List<ChatOutboxEvent> findByStatusOrderByCreatedAtAsc(ChatOutboxEventStatus status, Pageable pageable);

	@Modifying
	@Query("DELETE FROM ChatOutboxEvent e WHERE e.status = :status AND e.publishedAt < :threshold")
	void deleteByStatusAndPublishedAtBefore(
		@Param("status") ChatOutboxEventStatus status,
		@Param("threshold") LocalDateTime threshold
	);
}
