package com.rentalplatform.controller;

import com.rentalplatform.dto.*;
import com.rentalplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    private final String SIGN_UP = "/signup";
    private final String SIGN_IN = "/signin";
    private final String GET_PROFILE_INFO = "/me";
    private final String UPDATE_PROFILE = "/update";
    private final String DELETE_PROFILE = "/delete";

    @PostMapping(SIGN_UP)
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody RegisterDto registerDto) {
        return ResponseEntity.ok(userService.signUp(registerDto));
    }

    @PostMapping(SIGN_IN)
    public ResponseEntity<TokenResponse> signIn(@Valid @RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(userService.signIn(loginDto));
    }

    @GetMapping(GET_PROFILE_INFO)
    public ResponseEntity<UserDto> getProfileInfo(Principal principal) {
        return ResponseEntity.ok(userService.getProfileInfo(principal.getName()));
    }

    @PatchMapping(UPDATE_PROFILE)
    public ResponseEntity<UserDto> updateProfile(Principal principal, @Valid @RequestBody UpdateProfileDto dto) {
        return ResponseEntity.ok(userService.updateProfile(principal.getName(), dto));
    }

    @DeleteMapping(DELETE_PROFILE)
    public ResponseEntity<String> deleteProfile(Principal principal, @Valid @RequestBody DeleteProfileDto dto) {
        userService.deleteProfile(principal.getName(), dto);
        return ResponseEntity.ok("Your profile has been successfully deleted");
    }
}
