package com.eventorgateway.auth.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.eventorgateway.auth.dto.ReissueTokenDto;
import com.eventorgateway.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthClient {
	private final WebClient webClient;

	public Mono<ApiResponse<ReissueTokenDto>> reissueTokens(ReissueTokenDto request) {
		return webClient.post()
			.uri("/auth/reissue")
			.bodyValue(request)
			.retrieve()
			.bodyToMono(new ParameterizedTypeReference<ApiResponse<ReissueTokenDto>>() {
			});
	}

	public WebClient.ResponseSpec logout() {
		return webClient.post()
			.uri("/auth/logout")
			.retrieve();
	}
}
