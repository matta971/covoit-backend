package com.nc.sinpase.poc.modulith.covoit.identity.adapters.out.persistence;

import com.nc.sinpase.poc.modulith.covoit.identity.domain.PhoneNumber;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.Role;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.RoleRepository;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.User;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.UserRepository;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.UserStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class UserPersistenceAdapter implements UserRepository, RoleRepository {

    private final SpringDataUserRepository userRepo;
    private final SpringDataRoleRepository roleRepo;

    UserPersistenceAdapter(SpringDataUserRepository userRepo, SpringDataRoleRepository roleRepo) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
    }

    // --- UserRepository ---

    @Override
    public void save(User user) {
        UserJpaEntity entity = userRepo.findById(user.getId()).orElseGet(UserJpaEntity::new);
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setDisplayName(user.getDisplayName());
        entity.setPhoneDialCode(user.getPhoneNumber().dialCode());
        entity.setPhoneNumber(user.getPhoneNumber().number());
        entity.setStatus(user.getStatus().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setLastLoginAt(user.getLastLoginAt());

        Set<RoleJpaEntity> roleEntities = user.getRoles().stream()
                .map(role -> roleRepo.findById(role.getId())
                        .orElseThrow(() -> new IllegalStateException("Role not found: " + role.getName())))
                .collect(Collectors.toSet());
        entity.setRoles(roleEntities);

        userRepo.save(entity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    // --- RoleRepository ---

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepo.findByName(name).map(this::roleToDomain);
    }

    @Override
    public void save(Role role) {
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        roleRepo.save(entity);
    }

    // --- Mapping ---

    private User toDomain(UserJpaEntity e) {
        Set<Role> roles = e.getRoles().stream()
                .map(this::roleToDomain)
                .collect(Collectors.toSet());

        return User.reconstitute(
                e.getId(),
                e.getEmail(),
                e.getPasswordHash(),
                e.getDisplayName(),
                new PhoneNumber(e.getPhoneDialCode(), e.getPhoneNumber()),
                UserStatus.valueOf(e.getStatus()),
                roles,
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getLastLoginAt()
        );
    }

    private Role roleToDomain(RoleJpaEntity e) {
        return new Role(e.getId(), e.getName());
    }
}
