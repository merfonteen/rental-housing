package com.rentalplatform.controller;

import com.rentalplatform.dto.LoginDto;
import com.rentalplatform.dto.RegisterDto;
import com.rentalplatform.dto.UserDto;
import com.rentalplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    private final String SIGN_UP = "/signup";
    private final String SIGN_IN = "/signin";

    @PostMapping(SIGN_UP)
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody RegisterDto registerDto) {
        return ResponseEntity.ok(userService.signUp(registerDto));
    }

    @PostMapping(SIGN_IN)
    public ResponseEntity<?> signIn(@Valid @RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(userService.signIn(loginDto));
    }
}
