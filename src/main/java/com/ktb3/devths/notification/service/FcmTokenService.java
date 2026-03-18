package com.ktb3.devths.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.notification.domain.entity.FcmToken;
import com.ktb3.devths.notification.dto.request.FcmTokenRegisterRequest;
import com.ktb3.devths.notification.dto.request.FcmTokenUpdateRequest;
import com.ktb3.devths.notification.dto.response.FcmTokenRegisterResponse;
import com.ktb3.devths.notification.dto.response.FcmTokenUpdateResponse;
import com.ktb3.devths.notification.repository.FcmTokenRepository;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

	private final FcmTokenRepository fcmTokenRepository;
	private final UserRepository userRepository;

	@Transactional
	public FcmTokenRegisterResponse registerToken(Long userId, String deviceId,
		FcmTokenRegisterRequest request) {
		User user = userRepository.findByIdAndIsWithdrawFalse(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		FcmToken fcmToken = fcmTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
			.map(existing -> {
				existing.updateToken(request.token(), request.deviceType());
				log.info("FCM 토큰 갱신: userId = {}", userId);
				return existing;
			})
			.orElseGet(() -> {
				FcmToken newToken = FcmToken.builder()
					.user(user)
					.token(request.token())
					.deviceType(request.deviceType())
					.deviceId(deviceId)
					.build();
				log.info("FCM 토큰 등록: userId = {}", userId);
				return fcmTokenRepository.save(newToken);
			});

		return FcmTokenRegisterResponse.from(fcmToken);
	}

	@Transactional
	public void deleteToken(Long userId, String deviceId) {
		FcmToken fcmToken = fcmTokenRepository.findByDeviceId(deviceId)
			.orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

		if (!fcmToken.getUser().getId().equals(userId)) {
			throw new CustomException(ErrorCode.FCM_TOKEN_ACCESS_DENIED);
		}

		fcmTokenRepository.delete(fcmToken);
		log.info("FCM 토큰 삭제: userId={}", userId);
	}

	@Transactional
	public FcmTokenUpdateResponse updateTokenActive(Long userId, String deviceId,
		FcmTokenUpdateRequest request) {
		FcmToken fcmToken = fcmTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
			.orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

		fcmToken.updateActive(request.isActive());
		log.info("FCM 토큰 알림 설정 변경: userId={}", userId);

		return FcmTokenUpdateResponse.from(fcmToken);
	}
}
