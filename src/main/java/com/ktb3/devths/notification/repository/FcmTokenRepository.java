package com.ktb3.devths.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.notification.domain.entity.FcmToken;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

	Optional<FcmToken> findByUserIdAndDeviceId(Long userId, String deviceId);

	Optional<FcmToken> findByDeviceId(String deviceId);

	List<FcmToken> findAllByUserIdAndIsActiveTrue(Long userId);

	void deleteByToken(String token);
}
