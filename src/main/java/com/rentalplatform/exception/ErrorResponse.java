package com.rentalplatform.exception;

import lombok.*;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private Instant timestamp;
}
