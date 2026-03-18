package com.ktb3.devths.chat.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.config.properties.ChatRabbitProperties;
import com.ktb3.devths.chat.domain.constant.ChatOutboxEventType;
import com.ktb3.devths.chat.domain.entity.ChatOutboxEvent;
import com.ktb3.devths.chat.dto.internal.ChatTraceContext;
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
public class ChatOutboxEventRelayer {

	private static final int MAX_RETRY_COUNT = 3;
	private static final String METRIC_OUTBOX_PUBLISHED = "chat.outbox.published";
	private static final String METRIC_OUTBOX_FAILED = "chat.outbox.failed";

	private final ChatOutboxEventRepository chatOutboxEventRepository;
	private final RabbitTemplate rabbitTemplate;
	private final ChatRabbitProperties chatRabbitProperties;
	private final ObjectMapper objectMapper;
	private final MeterRegistry meterRegistry;
	private final Tracer tracer;
	private final Propagator propagator;

	@Transactional
	public void relayEvent(ChatOutboxEvent event) {
		Span publishSpan = restoreSpanContext(event);

		try (Tracer.SpanInScope ignored = tracer.withSpan(publishSpan)) {
			String routingKey = resolveRoutingKey(event.getEventType());
			Map<String, Object> headers = buildHeaders(event.getTraceContext());

			Message message = MessageBuilder
				.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
				.setContentType(MessageProperties.CONTENT_TYPE_JSON)
				.build();

			headers.forEach((key, value) -> message.getMessageProperties().setHeader(key, value));
			message.getMessageProperties().setHeader("eventType", event.getEventType().name());
			message.getMessageProperties().setHeader("aggregateType", event.getAggregateType());
			message.getMessageProperties().setHeader("aggregateId", event.getAggregateId());

			rabbitTemplate.send(chatRabbitProperties.getExchange(), routingKey, message);

			event.markPublished();
			chatOutboxEventRepository.save(event);
			incrementCounter(METRIC_OUTBOX_PUBLISHED, event.getEventType());
		} catch (Exception e) {
			publishSpan.error(e);
			event.incrementRetryCount();
			if (event.getRetryCount() >= MAX_RETRY_COUNT) {
				event.markFailed();
				incrementCounter(METRIC_OUTBOX_FAILED, event.getEventType());
				log.error("Outbox 이벤트 최종 실패: eventId={}, eventType={}",
					event.getId(), event.getEventType(), e);
			} else {
				log.warn("Outbox 이벤트 relay 실패 (retry={}): eventId={}",
					event.getRetryCount(), event.getId(), e);
			}
			chatOutboxEventRepository.save(event);
		} finally {
			publishSpan.end();
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

	private Span restoreSpanContext(ChatOutboxEvent event) {
		String traceContextJson = event.getTraceContext();
		Span.Builder spanBuilder;

		if (traceContextJson != null) {
			try {
				ChatTraceContext traceContext = objectMapper.readValue(traceContextJson, ChatTraceContext.class);
				if (traceContext.traceparent() != null) {
					Map<String, String> carrier = new HashMap<>();
					carrier.put("traceparent", traceContext.traceparent());
					putIfNotBlank(carrier, "tracestate", traceContext.tracestate());
					putIfNotBlank(carrier, "baggage", traceContext.baggage());
					spanBuilder = propagator.extract(carrier, Map::get);
				} else {
					spanBuilder = tracer.spanBuilder();
				}
			} catch (Exception e) {
				spanBuilder = tracer.spanBuilder();
			}
		} else {
			spanBuilder = tracer.spanBuilder();
		}

		return spanBuilder
			.name("chat.message.rabbit.publish")
			.kind(Span.Kind.PRODUCER)
			.tag("messaging.system", "rabbitmq")
			.tag("chat.outbox.event.id", String.valueOf(event.getId()))
			.tag("chat.outbox.event.type", event.getEventType().name())
			.start();
	}

	private void incrementCounter(String metricName, ChatOutboxEventType eventType) {
		Counter.builder(metricName)
			.tag("event_type", eventType.name())
			.register(meterRegistry)
			.increment();
	}

	private <V> void putIfNotBlank(Map<String, V> map, String key, String value) {
		if (value != null && !value.isBlank()) {
			@SuppressWarnings("unchecked")
			V castedValue = (V)value;
			map.put(key, castedValue);
		}
	}
}
