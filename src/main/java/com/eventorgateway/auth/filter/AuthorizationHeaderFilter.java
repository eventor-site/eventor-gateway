package com.eventorgateway.auth.filter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.eventorgateway.auth.client.AuthClient;
import com.eventorgateway.auth.dto.ReissueTokenDto;
import com.eventorgateway.auth.util.JwtUtils;
import com.eventorgateway.global.dto.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.jsonwebtoken.ExpiredJwtException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
	private final JwtUtils jwtUtils;
	private final AuthClient tokenClient;
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	public AuthorizationHeaderFilter(JwtUtils jwtUtils, AuthClient tokenClient) {
		super(Config.class);
		this.jwtUtils = jwtUtils;
		this.tokenClient = tokenClient;
	}

	public static class Config {
		// application.yml 파일에서 지정한 filter 의 Argument 값을 받는 부분
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			String accessToken = extractToken(exchange, "Access-Token");
			String refreshToken = extractToken(exchange, "Refresh-Token");

			if (accessToken == null || refreshToken == null) {
				return chain.filter(exchange);
			}

			try {
				// Access-Token, Refresh-Token 이 유효한 경우
				jwtUtils.getClaims(accessToken);
				jwtUtils.getClaims(refreshToken);

				addAuthorizationHeaders(exchange.getRequest(), accessToken);
			} catch (ExpiredJwtException ex) {
				// 액세스 토큰이 만료된 경우
				return reissueTokenAndRetry(exchange, chain, accessToken, refreshToken);
			} catch (Exception ex) {
				// 토큰이 유효하지 않은 경우
				return handleInvalidToken(exchange);
			}

			return chain.filter(exchange); // 토큰이 유효할 경우 체인 계속 진행
		};
	}

	// 액세스 토큰이 만료된 경우 재발급을 요청하는 메소드
	private Mono<Void> reissueTokenAndRetry(ServerWebExchange exchange, GatewayFilterChain chain, String accessToken,
		String refreshToken) {

		return tokenClient.reissueTokens(new ReissueTokenDto(accessToken, refreshToken))
			.flatMap(response -> {
				String newAccessToken = response.accessToken();
				String newRefreshToken = response.refreshToken();

				// 새로 발급된 토근 응답에 추가
				HttpHeaders httpHeaders = exchange.getResponse().getHeaders();
				httpHeaders.add("New-Access-Token",
					URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8));
				httpHeaders.add("New-Refresh-Token",
					URLEncoder.encode(newRefreshToken, StandardCharsets.UTF_8));

				ServerHttpRequest updatedRequest = exchange.getRequest().mutate()
					.header("Access-Token", newAccessToken)
					.header("Refresh-Token", newRefreshToken)
					.build();

				addAuthorizationHeaders(updatedRequest, newAccessToken);

				return chain.filter(exchange.mutate().request(updatedRequest).build()); // 재발급된 토큰으로 요청 이어서 진행
			})
			.onErrorResume(ex -> handleInvalidToken(exchange)); // 재발급 실패 시 에러 처리
	}

	// 헤더에 인증된 사용자 정보 추가하는 메소드
	private void addAuthorizationHeaders(ServerHttpRequest request, String accessToken) {
		request.mutate()
			.header("X-User-userId", jwtUtils.getUserIdFromToken(accessToken).toString())
			.header("X-User-Roles", jwtUtils.getRolesFromToken(accessToken).toString())
			.build();
	}

	// 토큰이 유효하지 않은 경우 처리하는 메소드
	private Mono<Void> handleInvalidToken(ServerWebExchange exchange) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

		ApiResponse<?> response = ApiResponse.createError("401", "토큰 검증에 실패하였습니다.");

		try {
			// ApiResponse 를 JSON 문자열로 변환
			String json = objectMapper.writeValueAsString(response);
			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

			return exchange.getResponse().writeWith(Flux.just(buffer));
		} catch (JsonProcessingException e) {
			return Mono.error(new RuntimeException("JSON 변환 실패", e));
		}
	}

	// // 토큰이 유효하지 않은 경우 처리하는 메소드
	// private Mono<Void> handleInvalidToken(ServerWebExchange exchange) {
	// 	exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
	// 	byte[] bytes = "토큰 검증에 실패하였습니다.".getBytes(StandardCharsets.UTF_8);
	// 	DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
	// 	return exchange.getResponse().writeWith(Flux.just(buffer));
	// }

	// 특정 헤더에서 토큰 값을 추출하는 메소드
	private String extractToken(ServerWebExchange exchange, String headerName) {
		List<String> headers = exchange.getRequest().getHeaders().get(headerName);
		if (headers == null || headers.isEmpty()) {
			return null;
		}

		return headers.getFirst().replace("Bearer+", ""); // "Bearer " 제거
	}

	// // 토큰 검증 요청 중 예외 상황에 따른 예외 처리 핸들러
	// @Bean
	// public ErrorWebExceptionHandler tokenValidation() {
	// 	return new JwtTokenExceptionHandler();
	// }
	//
	// public static class JwtTokenExceptionHandler implements ErrorWebExceptionHandler {
	// 	private String getErrorCode(int errorCode) {
	// 		return "{errorCode: " + errorCode + "}";
	// 	}
	//
	// 	@Override
	// 	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
	// 		int errorCode = 500;
	// 		if (ex instanceof NullPointerException) {
	// 			errorCode = 100;
	// 		} else if (ex instanceof ExpiredJwtException) {
	// 			errorCode = 200;
	// 		}
	//
	// 		byte[] bytes = getErrorCode(errorCode).getBytes(StandardCharsets.UTF_8);
	// 		DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
	// 		return exchange.getResponse().writeWith(Flux.just(buffer));
	// 	}
	// }
}
