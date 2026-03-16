package com.ktb3.devths.chat.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.config.properties.ChatRabbitProperties;
import com.ktb3.devths.chat.domain.constant.ChatOutboxEventType;
import com.ktb3.devths.chat.domain.entity.ChatOutboxEvent;
import com.ktb3.devths.chat.dto.internal.ChatTraceContext;
import com.ktb3.devths.chat.repository.ChatOutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOutboxEventRelayer {

	private static final int MAX_RETRY_COUNT = 3;

	private final ChatOutboxEventRepository chatOutboxEventRepository;
	private final RabbitTemplate rabbitTemplate;
	private final ChatRabbitProperties chatRabbitProperties;
	private final ObjectMapper objectMapper;

	@Transactional
	public void relayEvent(ChatOutboxEvent event) {
		try {
			String routingKey = resolveRoutingKey(event.getEventType());
			Map<String, Object> headers = buildHeaders(event.getTraceContext());

			rabbitTemplate.convertAndSend(
				chatRabbitProperties.getExchange(),
				routingKey,
				event.getPayload(),
				message -> {
					headers.forEach((key, value) -> message.getMessageProperties().setHeader(key, value));
					message.getMessageProperties().setHeader("eventType", event.getEventType().name());
					message.getMessageProperties().setHeader("aggregateType", event.getAggregateType());
					message.getMessageProperties().setHeader("aggregateId", event.getAggregateId());
					return message;
				}
			);

			event.markPublished();
			chatOutboxEventRepository.save(event);
		} catch (Exception e) {
			event.incrementRetryCount();
			if (event.getRetryCount() >= MAX_RETRY_COUNT) {
				event.markFailed();
				log.error("Outbox 이벤트 최종 실패: eventId={}, eventType={}",
					event.getId(), event.getEventType(), e);
			} else {
				log.warn("Outbox 이벤트 relay 실패 (retry={}): eventId={}",
					event.getRetryCount(), event.getId(), e);
			}
			chatOutboxEventRepository.save(event);
		}
	}

	private String resolveRoutingKey(ChatOutboxEventType eventType) {
		return switch (eventType) {
			case CHAT_MESSAGE, CHAT_MESSAGE_DELETED -> chatRabbitProperties.getMessageRoutingKey();
			case CHAT_NOTIFICATION -> chatRabbitProperties.getNotificationRoutingKey();
		};
	}

	private Map<String, Object> buildHeaders(String traceContextJson) {
		Map<String, Object> headers = new HashMap<>();
		if (traceContextJson == null) {
			return headers;
		}
		try {
			ChatTraceContext traceContext = objectMapper.readValue(traceContextJson, ChatTraceContext.class);
			putIfNotBlank(headers, "traceparent", traceContext.traceparent());
			putIfNotBlank(headers, "tracestate", traceContext.tracestate());
			putIfNotBlank(headers, "baggage", traceContext.baggage());
		} catch (Exception e) {
			log.warn("Trace context 파싱 실패", e);
		}
		return headers;
	}

	private void putIfNotBlank(Map<String, Object> headers, String key, String value) {
		if (value != null && !value.isBlank()) {
			headers.put(key, value);
		}
	}
}
