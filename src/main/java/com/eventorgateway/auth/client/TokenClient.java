package com.eventorgateway.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.eventorgateway.auth.dto.ReissueTokensDto;
import com.eventorgateway.auth.dto.response.ReissueTokensResponse;

import reactor.core.publisher.Mono;

@Component
public class TokenClient {
	private final WebClient webClient;

	@Value("${webClient.url}")
	private String webClientUrl;

	public TokenClient(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl(webClientUrl).build();
	}

	public Mono<ReissueTokensResponse> reissueTokens(ReissueTokensDto request) {
		return webClient.post()
			.uri("/auth/reissue-token")
			.bodyValue(request)
			.retrieve()
			.bodyToMono(ReissueTokensResponse.class);
	}

	public WebClient.ResponseSpec logout() {
		return webClient.post()
			.uri("/auth/logout")
			.retrieve();
	}
}
