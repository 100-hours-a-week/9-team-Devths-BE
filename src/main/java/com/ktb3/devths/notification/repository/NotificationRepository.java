package com.ktb3.devths.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.notification.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
