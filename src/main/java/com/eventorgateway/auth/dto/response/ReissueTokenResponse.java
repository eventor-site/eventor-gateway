package com.eventorgateway.auth.dto.response;

import lombok.Builder;

@Builder
public record ReissueTokenResponse(
	String accessToken,
	String refreshToken) {
}
