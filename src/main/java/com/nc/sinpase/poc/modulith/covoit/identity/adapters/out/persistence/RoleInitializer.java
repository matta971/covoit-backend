package com.nc.sinpase.poc.modulith.covoit.identity.adapters.out.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class RoleInitializer implements ApplicationRunner {

    private final SpringDataRoleRepository roleRepository;

    RoleInitializer(SpringDataRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            RoleJpaEntity role = new RoleJpaEntity();
            role.setId(UUID.randomUUID());
            role.setName("ROLE_USER");
            roleRepository.save(role);
        }
    }
}
