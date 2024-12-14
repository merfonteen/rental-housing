package com.rentalplatform.factory;

import com.rentalplatform.dto.UserDto;
import com.rentalplatform.entity.RoleEntity;
import com.rentalplatform.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserDtoFactory {
    public UserDto makeUserDto(UserEntity user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList()))
                .build();
    }
}
