package com.ktb3.devths.chat.event;

import java.util.List;

public record ChatOutboxCreatedEvent(
	List<Long> outboxEventIds
) {
}
