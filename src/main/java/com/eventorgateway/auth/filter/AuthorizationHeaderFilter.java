package com.eventorgateway.auth.filter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.sikyeojogateway.auth.client.TokenClient;
import com.sikyeojogateway.auth.dto.ReissueTokensDto;
import com.sikyeojogateway.auth.util.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
	private final JwtUtils jwtUtils;
	private final TokenClient tokenClient;

	public AuthorizationHeaderFilter(JwtUtils jwtUtils, TokenClient tokenClient) {
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
			String accessToken = extractToken(exchange, "Authorization");
			String refreshToken = extractToken(exchange, "Refresh-Token");

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
		return tokenClient.reissueTokens(new ReissueTokensDto(accessToken, refreshToken))
			.flatMap(response -> {
				String newAccessToken = response.accessToken();
				String newRefreshToken = response.refreshToken();

				// 새로 발급된 토근 응답에 추가
				exchange.getResponse().getHeaders().add("New-Authorization",
					URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8));
				exchange.getResponse().getHeaders().add("New-Refresh-Token",
					URLEncoder.encode(newRefreshToken, StandardCharsets.UTF_8));

				ServerHttpRequest updatedRequest = exchange.getRequest().mutate()
					.header("Authorization", newAccessToken)
					.header("Refresh-Token", newRefreshToken)
					.build();

				addAuthorizationHeaders(updatedRequest, newAccessToken);

				return chain.filter(exchange.mutate().request(updatedRequest).build()); // 재발급된 토큰으로 요청 다시 진행
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
		byte[] bytes = "유효하지 않은 토큰 입니다.".getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
		return exchange.getResponse().writeWith(Flux.just(buffer));
	}

	// 특정 헤더에서 토큰 값을 추출하는 메소드
	private String extractToken(ServerWebExchange exchange, String headerName) {
		List<String> headers = exchange.getRequest().getHeaders().get(headerName);
		if (headers == null || headers.isEmpty()) {
			return null;
		}

		return headers.getFirst().replace("Bearer+", ""); // "Bearer " 제거
	}

	// 토큰 검증 요청 중 예외 상황에 따른 예외 처리 핸들러
	@Bean
	public ErrorWebExceptionHandler tokenValidation() {
		return new JwtTokenExceptionHandler();
	}

	public static class JwtTokenExceptionHandler implements ErrorWebExceptionHandler {
		private String getErrorCode(int errorCode) {
			return "{errorCode: " + errorCode + "}";
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			int errorCode = 500;
			if (ex instanceof NullPointerException) {
				errorCode = 100;
			} else if (ex instanceof ExpiredJwtException) {
				errorCode = 200;
			}

			byte[] bytes = getErrorCode(errorCode).getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
			return exchange.getResponse().writeWith(Flux.just(buffer));
		}
	}
}
