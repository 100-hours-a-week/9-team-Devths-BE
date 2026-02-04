package com.ktb3.devths.notification.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.notification.domain.constant.NotificationCategory;
import com.ktb3.devths.notification.domain.constant.NotificationType;
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

	@Query("SELECT COUNT(n) FROM Notification n "
		+ "WHERE n.recipient.id = :recipientId "
		+ "AND n.isRead = false "
		+ "AND n.isDeleted = false")
	Long countUnreadByRecipientId(@Param("recipientId") Long recipientId);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true "
		+ "WHERE n.resourceId = :roomId "
		+ "AND n.category = :category "
		+ "AND n.type = :type "
		+ "AND n.isRead = false "
		+ "AND n.isDeleted = false")
	int markAsReadByRoomIdAndCategoryAndType(
		@Param("roomId") Long roomId,
		@Param("category") NotificationCategory category,
		@Param("type") NotificationType type
	);
}
