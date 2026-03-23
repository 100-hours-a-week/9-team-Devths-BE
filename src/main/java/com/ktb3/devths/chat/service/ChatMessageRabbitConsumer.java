package com.ktb3.devths.chat.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.chat.domain.constant.ChatOutboxEventType;
import com.ktb3.devths.chat.dto.response.ChatMessageResponse;
import com.ktb3.devths.chat.tracing.ChatTraceConstants;
import com.ktb3.devths.global.util.LogSanitizer;
import com.rabbitmq.client.Channel;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageRabbitConsumer {

	private static final String TOPIC_PREFIX = "/topic/chatroom/";
	private static final String METRIC_STOMP_DELIVERED = "chat.stomp.delivered";

	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final Tracer tracer;
	private final Propagator propagator;
	private final MeterRegistry meterRegistry;

	@RabbitListener(queues = "${chat.rabbitmq.message-queue}")
	public void onMessage(Message message, Channel channel) throws Exception {
		long deliveryTag = message.getMessageProperties().getDeliveryTag();
		String eventTypeHeader = getHeader(message, "eventType");
		String aggregateId = getHeader(message, "aggregateId");

		Span consumeSpan = createConsumeSpan(message, aggregateId);
		try (Tracer.SpanInScope ignored = tracer.withSpan(consumeSpan);
			BaggageInScope baggageInScope = openChatSessionBaggage(message)) {

			String body = new String(message.getBody());

			if (ChatOutboxEventType.CHAT_MESSAGE.name().equals(eventTypeHeader)) {
				ChatMessageResponse response = objectMapper.readValue(body, ChatMessageResponse.class);
				sendWithFanoutSpan(aggregateId, response);
			} else if (ChatOutboxEventType.CHAT_MESSAGE_DELETED.name().equals(eventTypeHeader)) {
				messagingTemplate.convertAndSend(TOPIC_PREFIX + aggregateId, body);
			}

			channel.basicAck(deliveryTag, false);
			Counter.builder(METRIC_STOMP_DELIVERED)
				.tag("type", "message")
				.register(meterRegistry)
				.increment();
		} catch (Exception e) {
			consumeSpan.error(e);
			channel.basicNack(deliveryTag, false, true);
			log.error("RabbitMQ 메시지 소비 실패: roomId={}", LogSanitizer.sanitize(aggregateId), e);
		} finally {
			consumeSpan.end();
		}
	}

	private void sendWithFanoutSpan(String roomId, ChatMessageResponse response) {
		Span fanoutSpan = tracer.spanBuilder()
			.name("chat.message.stomp.fanout")
			.kind(Span.Kind.PRODUCER)
			.tag("messaging.system", "stomp")
			.tag("messaging.destination", TOPIC_PREFIX + roomId)
			.tag("chat.room.id", LogSanitizer.sanitize(roomId))
			.tag("chat.message.id", String.valueOf(response.messageId()))
			.start();

		try (Tracer.SpanInScope ignored = tracer.withSpan(fanoutSpan)) {
			messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, response);
		} catch (RuntimeException e) {
			fanoutSpan.error(e);
			throw e;
		} finally {
			fanoutSpan.end();
		}
	}

	private Span createConsumeSpan(Message message, String roomId) {
		Map<String, String> carrier = extractTraceHeaders(message);
		Span.Builder spanBuilder = carrier.isEmpty()
			? tracer.spanBuilder()
			: propagator.extract(carrier, Map::get);

		return spanBuilder
			.name("chat.message.rabbit.consume")
			.kind(Span.Kind.CONSUMER)
			.tag("messaging.system", "rabbitmq")
			.tag("chat.room.id", LogSanitizer.sanitize(roomId))
			.start();
	}

	private Map<String, String> extractTraceHeaders(Message message) {
		Map<String, String> carrier = new HashMap<>();
		putHeaderIfPresent(carrier, message, "traceparent");
		putHeaderIfPresent(carrier, message, "tracestate");
		putHeaderIfPresent(carrier, message, "baggage");
		return carrier;
	}

	private BaggageInScope openChatSessionBaggage(Message message) {
		String baggage = getHeader(message, "baggage");
		if (baggage == null || baggage.isBlank()) {
			return BaggageInScope.NOOP;
		}
		return tracer.createBaggageInScope(ChatTraceConstants.CHAT_SESSION_BAGGAGE_KEY, baggage);
	}

	private String getHeader(Message message, String key) {
		Object value = message.getMessageProperties().getHeader(key);
		return value != null ? value.toString() : null;
	}

	private void putHeaderIfPresent(Map<String, String> carrier, Message message, String key) {
		String value = getHeader(message, key);
		if (value != null && !value.isBlank()) {
			carrier.put(key, value);
		}
	}
}
