package com.rentalplatform.utils;

import com.rentalplatform.entity.RoleEntity;
import com.rentalplatform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {

        if (!roleRepository.existsByName("TENANT")) {
            roleRepository.save(RoleEntity.builder().name("TENANT").build());
        }


        if (!roleRepository.existsByName("LANDLORD")) {
            roleRepository.save(RoleEntity.builder().name("LANDLORD").build());
        }
    }
}
