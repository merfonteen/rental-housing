package com.rentalplatform.service;

import com.rentalplatform.dto.LoginDto;
import com.rentalplatform.dto.RegisterDto;
import com.rentalplatform.dto.TokenResponse;
import com.rentalplatform.dto.UserDto;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.UserDtoFactory;
import com.rentalplatform.security.JwtTokenUtil;
import com.rentalplatform.entity.RoleEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.repository.RoleRepository;
import com.rentalplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserDtoFactory userDtoFactory;
    private final RoleRepository roleRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserDto signUp(RegisterDto registerDto) {
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new BadRequestException("Email is already taken");
        }

        RoleEntity role = roleRepository.findByName("TENANT")
                .orElseThrow(() -> new NotFoundException("Role 'TENANT' not found"));

        UserEntity user = UserEntity.builder()
                .username(registerDto.getUsername())
                .email(registerDto.getEmail())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .roles(List.of(role))
                .build();

        UserEntity savedUser = userRepository.save(user);

        return userDtoFactory.makeUserDto(savedUser);
    }

    public TokenResponse signIn(LoginDto loginDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            return TokenResponse.builder()
                    .token(jwtTokenUtil.generateToken(userDetails))
                    .build();
        }
        catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid password or email");
        }
    }
}
