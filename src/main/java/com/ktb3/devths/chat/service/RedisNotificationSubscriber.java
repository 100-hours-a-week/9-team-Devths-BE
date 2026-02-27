package com.ktb3.devths.chat.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.dto.response.ChatRoomNotification;
import com.ktb3.devths.global.util.LogSanitizer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisNotificationSubscriber implements MessageListener {

	private static final String CHANNEL_PREFIX = "chat:notify:";
	private static final String TOPIC_PREFIX = "/topic/user/";
	private static final String TOPIC_SUFFIX = "/notifications";
	private static final String OBSERVATION_NAME = "redis.pubsub";

	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObservationRegistry observationRegistry;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String userId = "unknown";

		Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.lowCardinalityKeyValue("operation", "subscribe")
			.lowCardinalityKeyValue("type", "notification")
			.start();

		try {
			String channel = new String(message.getChannel());
			String body = new String(message.getBody());

			userId = channel.replace(CHANNEL_PREFIX, "");
			observation.highCardinalityKeyValue("userId", LogSanitizer.sanitize(userId));

			ChatRoomNotification notification = objectMapper.readValue(body, ChatRoomNotification.class);

			messagingTemplate.convertAndSend(TOPIC_PREFIX + userId + TOPIC_SUFFIX, notification);

			observation.lowCardinalityKeyValue("outcome", "success");
			log.debug("Redis 알림 수신 성공: userId={}", LogSanitizer.sanitize(userId));
		} catch (Exception e) {
			observation.highCardinalityKeyValue("userId", LogSanitizer.sanitize(userId));
			observation.lowCardinalityKeyValue("outcome", "failure");
			observation.error(e);
			log.error("Redis 알림 수신 실패: userId={}", LogSanitizer.sanitize(userId), e);
		} finally {
			observation.stop();
		}
	}
}
