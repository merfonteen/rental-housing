package com.rentalplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RentalPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentalPlatformApplication.class, args);
    }
}
