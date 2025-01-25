package com.rentalplatform.mapper;

import com.rentalplatform.dto.UserDto;
import com.rentalplatform.entity.RoleEntity;
import com.rentalplatform.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserDtoMapper {
    public UserDto makeUserDto(UserEntity user, boolean includeRating) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .rating(includeRating ? user.getRating() : null)
                .roles(user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList()))
                .build();
    }
}
