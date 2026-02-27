package com.ktb3.devths.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.dto.response.ChatMessageResponse;
import com.ktb3.devths.chat.dto.response.ChatRoomNotification;
import com.ktb3.devths.global.util.LogSanitizer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

	private static final String CHANNEL_PREFIX = "chat:room:";
	private static final String NOTIFY_PREFIX = "chat:notify:";
	private static final String OBSERVATION_NAME = "redis.pubsub";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final ObservationRegistry observationRegistry;

	public void publish(Long roomId, ChatMessageResponse response) {
		String sanitizedRoomId = LogSanitizer.sanitize(String.valueOf(roomId));

		Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.lowCardinalityKeyValue("operation", "publish")
			.lowCardinalityKeyValue("type", "message")
			.highCardinalityKeyValue("roomId", sanitizedRoomId)
			.start();

		try {
			String channel = CHANNEL_PREFIX + roomId;
			String message = objectMapper.writeValueAsString(response);
			redisTemplate.convertAndSend(channel, message);

			observation.lowCardinalityKeyValue("outcome", "success");
			log.debug("Redis 메시지 발행 성공: roomId={}", sanitizedRoomId);
		} catch (Exception e) {
			observation.lowCardinalityKeyValue("outcome", "failure");
			observation.error(e);
			log.error("Redis 메시지 발행 실패: roomId={}", sanitizedRoomId, e);
		} finally {
			observation.stop();
		}
	}

	public void publishNotification(Long userId, ChatRoomNotification notification) {
		String sanitizedUserId = LogSanitizer.sanitize(String.valueOf(userId));

		Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.lowCardinalityKeyValue("operation", "publish")
			.lowCardinalityKeyValue("type", "notification")
			.highCardinalityKeyValue("userId", sanitizedUserId)
			.start();

		try {
			String channel = NOTIFY_PREFIX + userId;
			String message = objectMapper.writeValueAsString(notification);
			redisTemplate.convertAndSend(channel, message);

			observation.lowCardinalityKeyValue("outcome", "success");
			log.debug("Redis 알림 발행 성공: userId={}", sanitizedUserId);
		} catch (Exception e) {
			observation.lowCardinalityKeyValue("outcome", "failure");
			observation.error(e);
			log.error("Redis 알림 발행 실패: userId={}", sanitizedUserId, e);
		} finally {
			observation.stop();
		}
	}
}
