package com.rentalplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DeleteProfileDto {
    @NotBlank(message = "You need to confirm your password to delete your profile")
    private String password;
}
