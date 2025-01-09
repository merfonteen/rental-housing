package com.rentalplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private Double rating;
    private List<String> roles;
}
