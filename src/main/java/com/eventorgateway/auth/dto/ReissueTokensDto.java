package com.eventorgateway.auth.dto;

import lombok.Builder;

@Builder
public record ReissueTokensDto(
	String accessToken,
	String refreshToken) {
}
