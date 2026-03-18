package com.ktb3.devths.chat.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.rabbitmq")
public class ChatRabbitProperties {
	private String exchange = "chat.events";
	private String messageQueue = "chat.message.queue";
	private String notificationQueue = "chat.notification.queue";
	private String messageDlq = "chat.message.dlq";
	private String notificationDlq = "chat.notification.dlq";
	private String messageRoutingKey = "chat.message.broadcast";
	private String notificationRoutingKey = "chat.notification.user";
}
