package com.ktb3.devths.chat.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ktb3.devths.chat.config.properties.ChatRabbitProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ChatRabbitMqConfig {

	private final ChatRabbitProperties properties;

	@Bean
	public TopicExchange chatExchange() {
		return new TopicExchange(properties.getExchange(), true, false);
	}

	@Bean
	public Queue chatMessageQueue() {
		return QueueBuilder.durable(properties.getMessageQueue())
			.withArgument("x-dead-letter-exchange", "")
			.withArgument("x-dead-letter-routing-key", properties.getMessageDlq())
			.build();
	}

	@Bean
	public Queue chatNotificationQueue() {
		return QueueBuilder.durable(properties.getNotificationQueue())
			.withArgument("x-dead-letter-exchange", "")
			.withArgument("x-dead-letter-routing-key", properties.getNotificationDlq())
			.build();
	}

	@Bean
	public Queue chatMessageDlq() {
		return QueueBuilder.durable(properties.getMessageDlq()).build();
	}

	@Bean
	public Queue chatNotificationDlq() {
		return QueueBuilder.durable(properties.getNotificationDlq()).build();
	}

	@Bean
	public Binding chatMessageBinding(Queue chatMessageQueue, TopicExchange chatExchange) {
		return BindingBuilder.bind(chatMessageQueue).to(chatExchange).with("chat.message.#");
	}

	@Bean
	public Binding chatNotificationBinding(Queue chatNotificationQueue, TopicExchange chatExchange) {
		return BindingBuilder.bind(chatNotificationQueue).to(chatExchange).with("chat.notification.#");
	}

	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
