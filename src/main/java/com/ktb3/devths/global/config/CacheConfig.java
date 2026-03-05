package com.ktb3.devths.global.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.board.dto.response.PostDetailResponse;
import com.ktb3.devths.global.config.properties.AppCacheProperties;

@Configuration
@EnableCaching
@ConditionalOnClass(RedisCacheManager.class)
public class CacheConfig {

	public static final String POST_DETAIL_CACHE = "postDetail";

	@Bean
	public CacheManager cacheManager(
		RedisConnectionFactory redisConnectionFactory,
		ObjectMapper objectMapper,
		AppCacheProperties appCacheProperties
	) {
		if (!appCacheProperties.isEnabled() || !appCacheProperties.getPostDetail().isEnabled()) {
			return new NoOpCacheManager();
		}

		GenericJackson2JsonRedisSerializer defaultSerializer = new GenericJackson2JsonRedisSerializer(objectMapper.copy());

		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues()
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer))
			.entryTtl(Duration.ofMinutes(5));

		Jackson2JsonRedisSerializer<PostDetailResponse> postDetailSerializer =
			new Jackson2JsonRedisSerializer<>(objectMapper, PostDetailResponse.class);

		RedisCacheConfiguration postDetailConfig = RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues()
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(postDetailSerializer))
			.entryTtl(appCacheProperties.getPostDetail().getTtl());

		Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
		cacheConfigs.put(POST_DETAIL_CACHE, postDetailConfig);

		return RedisCacheManager.builder(redisConnectionFactory)
			.cacheDefaults(defaultConfig)
			.withInitialCacheConfigurations(cacheConfigs)
			.build();
	}
}
