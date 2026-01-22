package com.ktb3.devths.chatbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;

public interface AiChatRoomRepository extends JpaRepository<AiChatRoom, Long> {

	Optional<AiChatRoom> findByIdAndIsDeletedFalse(Long id);

	@Query("SELECT r FROM AiChatRoom r " + "WHERE r.user.id = :userId " + "AND r.isDeleted = false " + "ORDER BY r.updatedAt DESC, r.id DESC")
	List<AiChatRoom> findByUserIdAndNotDeleted(
		@Param("userId") Long userId,
		Pageable pageable
	);

	@Query("SELECT r FROM AiChatRoom r " + "WHERE r.user.id = :userId " + "AND r.isDeleted = false " + "AND r.id < :lastId " + "ORDER BY r.updatedAt DESC, r.id DESC")
	List<AiChatRoom> findByUserIdAndNotDeletedAfterCursor(
		@Param("userId") Long userId,
		@Param("lastId") Long lastId,
		Pageable pageable
	);
}
