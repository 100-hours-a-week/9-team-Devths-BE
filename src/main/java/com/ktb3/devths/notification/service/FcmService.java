package com.ktb3.devths.notification.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.ktb3.devths.notification.domain.entity.FcmToken;
import com.ktb3.devths.notification.repository.FcmTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

	private final FirebaseMessaging firebaseMessaging;
	private final FcmTokenRepository fcmTokenRepository;

	@Async("taskExecutor")
	public void sendPushNotification(Long recipientId, String title, String body, String targetPath) {
		if (firebaseMessaging == null) {
			return;
		}

		List<FcmToken> tokens = fcmTokenRepository.findAllByUserIdAndIsActiveTrue(recipientId);
		if (tokens.isEmpty()) {
			return;
		}

		for (FcmToken fcmToken : tokens) {
			try {
				Message message = Message.builder()
					.setToken(fcmToken.getToken())
					.setWebpushConfig(WebpushConfig.builder()
						.setNotification(WebpushNotification.builder()
							.setTitle(title)
							.setBody(body)
							.build())
						.putData("targetPath", targetPath)
						.build())
					.build();

				firebaseMessaging.send(message);
			} catch (FirebaseMessagingException e) {
				handleMessagingException(fcmToken, e);
			}
		}
	}

	@Transactional
	protected void handleMessagingException(FcmToken fcmToken, FirebaseMessagingException ex) {
		MessagingErrorCode errorCode = ex.getMessagingErrorCode();

		if (errorCode == MessagingErrorCode.UNREGISTERED
			|| errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
			fcmTokenRepository.deleteByToken(fcmToken.getToken());
			log.info("만료된 FCM 토큰 삭제: userId={}", fcmToken.getUser().getId());
		} else {
			log.warn("FCM 전송 실패: userId={}, errorCode={}",
				fcmToken.getUser().getId(), errorCode);
		}
	}
}
