package com.ktb3.devths.notification.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.notification.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("SELECT n FROM Notification n "
		+ "LEFT JOIN FETCH n.sender "
		+ "WHERE n.recipient.id = :recipientId "
		+ "AND n.isDeleted = false "
		+ "ORDER BY n.createdAt DESC, n.id DESC")
	List<Notification> findByRecipientIdAndNotDeleted(
		@Param("recipientId") Long recipientId,
		Pageable pageable
	);

	@Query("SELECT n FROM Notification n "
		+ "LEFT JOIN FETCH n.sender "
		+ "WHERE n.recipient.id = :recipientId "
		+ "AND n.isDeleted = false "
		+ "AND n.id < :lastId "
		+ "ORDER BY n.createdAt DESC, n.id DESC")
	List<Notification> findByRecipientIdAndNotDeletedAfterCursor(
		@Param("recipientId") Long recipientId,
		@Param("lastId") Long lastId,
		Pageable pageable
	);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids")
	void bulkUpdateReadStatus(@Param("ids") List<Long> ids);
}
