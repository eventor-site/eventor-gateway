package com.eventorgateway.global.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.eventorgateway.auth.filter.AuthorizationHeaderFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {
	private final AuthorizationHeaderFilter authorizationHeaderFilter;

	@Profile("dev")
	@Bean
	public RouteLocator devRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			.route("eventor-back", r -> r.path("/back/users/signup/**", "/back/users/recover/**")
				.uri("http://localhost:8101")
			)
			.route("eventor-back", r -> r.path("/back/**")
				.filters(f -> f.filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
				.uri("http://localhost:8101")
			)
			.route("eventor-auth", r -> r.path("/auth/**", "/oauth2/**")
				.uri("http://localhost:8070")
			)
			.build();
	}

	@Profile("prod")
	@Bean
	public RouteLocator prodRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			.route("eventor-back", r -> r.path("/back/users/signup/**", "/back/users/recover/**")
				.uri("lb://eventor-back") // 컨테이너 이름 사용
			)
			.route("eventor-back", r -> r.path("/back/**")
				.filters(f -> f.filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
				.uri("lb://eventor-back") // 컨테이너 이름 사용
			)
			.route("eventor-auth", r -> r.path("/auth/**", "/oauth2/**")
				.uri("http://eventor-auth:8070") // 컨테이너 이름 사용
			)
			.build();
	}

}