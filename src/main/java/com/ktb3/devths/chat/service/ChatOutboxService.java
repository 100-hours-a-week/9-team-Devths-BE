package com.ktb3.devths.chat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.domain.constant.ChatOutboxEventType;
import com.ktb3.devths.chat.domain.entity.ChatOutboxEvent;
import com.ktb3.devths.chat.dto.internal.ChatTraceContext;
import com.ktb3.devths.chat.dto.response.ChatMessageResponse;
import com.ktb3.devths.chat.dto.response.ChatRoomNotification;
import com.ktb3.devths.chat.event.ChatOutboxCreatedEvent;
import com.ktb3.devths.chat.repository.ChatOutboxEventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOutboxService {

	private static final String AGGREGATE_CHAT_ROOM = "CHAT_ROOM";
	private static final String AGGREGATE_CHAT_USER = "CHAT_USER";

	private static final String METRIC_OUTBOX_CREATED = "chat.outbox.create.count";

	private final ChatOutboxEventRepository chatOutboxEventRepository;
	private final ObjectMapper objectMapper;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Tracer tracer;
	private final Propagator propagator;
	private final MeterRegistry meterRegistry;

	public void createMessageEvent(Long roomId, ChatMessageResponse response, String chatSessionId) {
		Span span = tracer.spanBuilder()
			.name("chat.message.outbox.persist")
			.kind(Span.Kind.PRODUCER)
			.tag("chat.room.id", String.valueOf(roomId))
			.tag("chat.message.id", String.valueOf(response.messageId()))
			.tag("chat.session.id", chatSessionId)
			.start();

		try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
			ChatOutboxEvent event = ChatOutboxEvent.builder()
				.eventType(ChatOutboxEventType.CHAT_MESSAGE)
				.aggregateType(AGGREGATE_CHAT_ROOM)
				.aggregateId(roomId)
				.payload(toJson(response))
				.traceContext(toJson(buildTraceContext(span)))
				.build();

			chatOutboxEventRepository.save(event);
			incrementCreatedCounter(ChatOutboxEventType.CHAT_MESSAGE);
			registerAfterCommitTrigger(List.of(event.getId()));
		} catch (Exception e) {
			span.error(e);
			log.error("메시지 outbox 이벤트 생성 실패: roomId={}", roomId, e);
			throw new RuntimeException("Outbox 이벤트 생성 실패", e);
		} finally {
			span.end();
		}
	}

	public void createNotificationEvents(List<Long> userIds, ChatRoomNotification notification,
		String chatSessionId) {
		List<Long> eventIds = new ArrayList<>();

		for (Long userId : userIds) {
			ChatOutboxEvent event = ChatOutboxEvent.builder()
				.eventType(ChatOutboxEventType.CHAT_NOTIFICATION)
				.aggregateType(AGGREGATE_CHAT_USER)
				.aggregateId(userId)
				.payload(toJson(notification))
				.traceContext(toJson(buildTraceContext()))
				.build();

			chatOutboxEventRepository.save(event);
			incrementCreatedCounter(ChatOutboxEventType.CHAT_NOTIFICATION);
			eventIds.add(event.getId());
		}

		registerAfterCommitTrigger(eventIds);
	}

	public void createDeletedMessageEvent(Long roomId, Long messageId, String chatSessionId) {
		ChatOutboxEvent event = ChatOutboxEvent.builder()
			.eventType(ChatOutboxEventType.CHAT_MESSAGE_DELETED)
			.aggregateType(AGGREGATE_CHAT_ROOM)
			.aggregateId(roomId)
			.payload(toJson(Map.of("roomId", roomId, "messageId", messageId)))
			.traceContext(toJson(buildTraceContext()))
			.build();

		chatOutboxEventRepository.save(event);
		incrementCreatedCounter(ChatOutboxEventType.CHAT_MESSAGE_DELETED);
		registerAfterCommitTrigger(List.of(event.getId()));
	}

	private void incrementCreatedCounter(ChatOutboxEventType eventType) {
		Counter.builder(METRIC_OUTBOX_CREATED)
			.tag("event_type", eventType.name())
			.register(meterRegistry)
			.increment();
	}

	private void registerAfterCommitTrigger(List<Long> eventIds) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				applicationEventPublisher.publishEvent(new ChatOutboxCreatedEvent(eventIds));
			}
		});
	}

	private ChatTraceContext buildTraceContext(Span span) {
		Map<String, String> carrier = new HashMap<>();
		propagator.inject(span.context(), carrier, Map::put);

		return new ChatTraceContext(
			carrier.get("traceparent"),
			carrier.get("tracestate"),
			carrier.get("baggage")
		);
	}

	private ChatTraceContext buildTraceContext() {
		Span currentSpan = tracer.currentSpan();
		if (currentSpan == null) {
			return new ChatTraceContext(null, null, null);
		}
		return buildTraceContext(currentSpan);
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON 직렬화 실패", e);
		}
	}
}
