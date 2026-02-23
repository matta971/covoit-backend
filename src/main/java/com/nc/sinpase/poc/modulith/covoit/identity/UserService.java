package com.nc.sinpase.poc.modulith.covoit.identity;

import com.nc.sinpase.poc.modulith.covoit.identity.domain.PhoneNumber;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.Role;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.RoleRepository;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.User;
import com.nc.sinpase.poc.modulith.covoit.identity.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public UserView createUser(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException(command.email());
        }

        PhoneNumber phone = new PhoneNumber(command.phoneDialCode(), command.phoneNumber());
        User user = User.create(command.email(), command.passwordHash(), command.displayName(), phone);

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_ROLE + " not found in database"));
        user.assignRole(defaultRole);

        userRepository.save(user);
        return toView(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserView> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toView);
    }

    @Transactional(readOnly = true)
    public Optional<UserView> findById(UUID userId) {
        return userRepository.findById(userId).map(this::toView);
    }

    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return userRepository.findByEmail(email).map(user -> new UserCredentials(
                user.getId(),
                user.getPasswordHash(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toUnmodifiableSet())
        ));
    }

    public void recordLogin(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.recordLogin(Instant.now());
            userRepository.save(user);
        });
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhoneNumber().dialCode(),
                user.getPhoneNumber().number(),
                user.getStatus().name(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toUnmodifiableSet())
        );
    }
}
