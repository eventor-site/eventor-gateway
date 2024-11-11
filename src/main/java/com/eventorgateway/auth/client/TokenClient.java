package com.eventorgateway.auth.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.sikyeojogateway.auth.dto.ReissueTokensDto;
import com.sikyeojogateway.auth.dto.response.ReissueTokensResponse;

import reactor.core.publisher.Mono;

@Component
public class TokenClient {
	private final WebClient webClient;

	public TokenClient(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl("http://localhost:8070").build();
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
