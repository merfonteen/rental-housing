package com.rentalplatform.dto.updateDto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateProfileDto {
    private String email;
    private String username;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String currentPassword;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
