package com.eventorgateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.eventorgateway.auth.filter.AuthorizationHeaderFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {
	private final AuthorizationHeaderFilter authorizationHeaderFilter;

	@Bean
	public RouteLocator prodRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			// .route("bookstore-back", r -> r.path("/back/**")
			//         .uri("lb://book-store-back") // 로드밸런싱 활성화
			// )
			.route("eventor-back", r -> r.path("/back/users/signUp/**")
				.uri("http://localhost:8083")
			)
			.route("eventor-back", r -> r.path("/back/**")
				.filters(f -> f.filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
				.uri("http://localhost:8083")
			)
			.route("eventor-auth", r -> r.path("/auth/**")
				.uri("http://localhost:8070")
			)
			.build();
	}

}