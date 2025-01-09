package com.rentalplatform.service;

import com.rentalplatform.dto.*;
import com.rentalplatform.entity.RoleEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.UserDtoFactory;
import com.rentalplatform.repository.RoleRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.security.JwtTokenUtil;
import jakarta.transaction.Transactional;
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
    private final RoleRepository roleRepository;
    private final UserDtoFactory userDtoFactory;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
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

        return userDtoFactory.makeUserDto(savedUser, false);
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
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid password or email");
        }
    }

    public UserDto getProfileInfo(String username) {
        UserEntity currentUser = getCurrentUser(username);
        return userDtoFactory.makeUserDto(currentUser, true);
    }

    @Transactional
    public UserDto getLandlordRole(String username) {
        UserEntity user = getCurrentUser(username);
        RoleEntity landlordRole = roleRepository.findByName("LANDLORD")
                .orElseThrow(() -> new NotFoundException("Role not found"));

        user.getRoles().add(landlordRole);

        UserEntity savedUser = userRepository.save(user);

        return userDtoFactory.makeUserDto(savedUser, false);
    }

    @Transactional
    public UserDto updateProfile(String username, UpdateProfileDto dto) {
        UserEntity currentUser = getCurrentUser(username);

        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            if (!dto.getUsername().equals(username) && userRepository.existsByUsername(dto.getUsername())) {
                throw new BadRequestException("User with name '%s' already exists".formatted(dto.getUsername()));
            }
            currentUser.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            if (!dto.getEmail().equals(currentUser.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                throw new BadRequestException("User with email '%s' already exists".formatted(dto.getEmail()));
            }
            currentUser.setEmail(dto.getEmail());
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isEmpty()) {
            if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isEmpty()) {
                throw new BadRequestException("To change your password you need to specify your  current password");
            }
            if (!passwordEncoder.matches(dto.getCurrentPassword(), currentUser.getPassword())) {
                throw new BadRequestException("The current password is incorrect");
            }
            currentUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        userRepository.save(currentUser);

        return userDtoFactory.makeUserDto(currentUser, false);
    }

    @Transactional
    public void deleteProfile(String username, DeleteProfileDto deleteDto) {
        UserEntity currentUser = getCurrentUser(username);

        if(!passwordEncoder.matches(deleteDto.getPassword(), currentUser.getPassword())) {
            throw new BadRequestException("The current password is incorrect");
        }

        userRepository.delete(currentUser);
    }

    private UserEntity getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));
    }
}
