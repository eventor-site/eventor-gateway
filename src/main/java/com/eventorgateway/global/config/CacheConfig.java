package com.eventorgateway.global.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeine(caffeineConfig());
		return cacheManager;
	}

	@Bean
	public com.github.benmanes.caffeine.cache.Caffeine<Object, Object> caffeineConfig() {
		return com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
			.expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
			.maximumSize(1000);
	}
}