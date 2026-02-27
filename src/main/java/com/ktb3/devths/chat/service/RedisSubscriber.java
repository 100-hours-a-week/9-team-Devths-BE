package com.ktb3.devths.chat.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.dto.response.ChatMessageResponse;
import com.ktb3.devths.global.util.LogSanitizer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

	private static final String CHANNEL_PREFIX = "chat:room:";
	private static final String TOPIC_PREFIX = "/topic/chatroom/";
	private static final String OBSERVATION_NAME = "redis.pubsub";

	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObservationRegistry observationRegistry;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String roomId = "unknown";

		Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.lowCardinalityKeyValue("operation", "subscribe")
			.lowCardinalityKeyValue("type", "message")
			.start();

		try {
			String channel = new String(message.getChannel());
			String body = new String(message.getBody());

			roomId = channel.replace(CHANNEL_PREFIX, "");
			observation.highCardinalityKeyValue("roomId", LogSanitizer.sanitize(roomId));

			ChatMessageResponse response = objectMapper.readValue(body, ChatMessageResponse.class);

			messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, response);

			observation.lowCardinalityKeyValue("outcome", "success");
			log.debug("Redis 메시지 수신 성공: roomId={}", LogSanitizer.sanitize(roomId));
		} catch (Exception e) {
			observation.highCardinalityKeyValue("roomId", LogSanitizer.sanitize(roomId));
			observation.lowCardinalityKeyValue("outcome", "failure");
			observation.error(e);
			log.error("Redis 메시지 수신 실패: roomId={}", LogSanitizer.sanitize(roomId), e);
		} finally {
			observation.stop();
		}
	}
}
