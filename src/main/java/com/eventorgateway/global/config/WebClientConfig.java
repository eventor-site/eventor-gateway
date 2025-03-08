package com.eventorgateway.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Value("${webClient.url}")
	private String baseUrl;

	@Bean
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder.baseUrl(baseUrl).build();
	}
}
