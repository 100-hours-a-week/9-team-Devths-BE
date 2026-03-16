package com.ktb3.devths.chat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ktb3.devths.chat.domain.constant.ChatOutboxEventStatus;
import com.ktb3.devths.chat.domain.entity.ChatOutboxEvent;
import com.ktb3.devths.chat.event.ChatOutboxCreatedEvent;
import com.ktb3.devths.chat.repository.ChatOutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOutboxRelayService {

	private static final int FALLBACK_BATCH_SIZE = 50;
	private static final int CLEANUP_DAYS = 7;

	private final ChatOutboxEventRepository chatOutboxEventRepository;
	private final ChatOutboxEventRelayer chatOutboxEventRelayer;

	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOutboxCreated(ChatOutboxCreatedEvent event) {
		List<ChatOutboxEvent> outboxEvents = chatOutboxEventRepository.findAllById(event.outboxEventIds());
		for (ChatOutboxEvent outboxEvent : outboxEvents) {
			chatOutboxEventRelayer.relayEvent(outboxEvent);
		}
	}

	@Scheduled(fixedDelay = 5000)
	public void pollPendingEvents() {
		List<ChatOutboxEvent> pendingEvents = chatOutboxEventRepository
			.findByStatusOrderByCreatedAtAsc(ChatOutboxEventStatus.PENDING, PageRequest.of(0, FALLBACK_BATCH_SIZE));

		for (ChatOutboxEvent event : pendingEvents) {
			chatOutboxEventRelayer.relayEvent(event);
		}
	}

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void cleanupPublishedEvents() {
		LocalDateTime threshold = LocalDateTime.now().minusDays(CLEANUP_DAYS);
		chatOutboxEventRepository.deleteByStatusAndPublishedAtBefore(ChatOutboxEventStatus.PUBLISHED, threshold);
		log.info("Outbox cleanup 완료: {}일 이전 PUBLISHED 이벤트 삭제", CLEANUP_DAYS);
	}
}
