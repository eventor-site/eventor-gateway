package com.eventorgateway.auth.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.eventorgateway.auth.dto.ReissueTokenDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthClient {
	private final WebClient webClient;

	public Mono<ReissueTokenDto> reissueTokens(ReissueTokenDto request) {
		return webClient.post()
			.uri("/auth/reissue")
			.bodyValue(request)
			.retrieve()
			.bodyToMono(ReissueTokenDto.class);
	}

	public WebClient.ResponseSpec logout() {
		return webClient.post()
			.uri("/auth/logout")
			.retrieve();
	}
}
