package com.ktb3.devths.chat.dto.internal;

public record ChatTraceContext(
	String traceparent,
	String tracestate,
	String baggage
) {
}
