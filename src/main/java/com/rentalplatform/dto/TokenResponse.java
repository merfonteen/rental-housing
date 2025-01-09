package com.rentalplatform.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
