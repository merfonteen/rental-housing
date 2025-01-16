package com.rentalplatform.controller;

import com.rentalplatform.dto.TokenResponse;
import com.rentalplatform.security.JwtTokenUtil;
import com.rentalplatform.security.CustomUserDetailService;
import com.rentalplatform.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class RefreshTokenController {

    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailService customUserDetailService;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshAccessToken(@RequestBody String refreshToken) {
        refreshTokenService.verifyRefreshToken(refreshToken);

        String username = jwtTokenUtil.extractUsername(refreshToken);
        UserDetails userDetails = customUserDetailService.loadUserByUsername(username);
        String newAccessToken = jwtTokenUtil.generateToken(userDetails);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();

        return ResponseEntity.ok(tokenResponse);
    }
}
