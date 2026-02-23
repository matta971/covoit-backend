package com.nc.sinpase.poc.modulith.covoit.auth;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nc.sinpase.poc.modulith.covoit.auth.domain.RefreshSession;
import com.nc.sinpase.poc.modulith.covoit.auth.domain.RefreshSessionRepository;
import com.nc.sinpase.poc.modulith.covoit.auth.domain.TokenIssuer;
import com.nc.sinpase.poc.modulith.covoit.identity.CreateUserCommand;
import com.nc.sinpase.poc.modulith.covoit.identity.UserCredentials;
import com.nc.sinpase.poc.modulith.covoit.identity.UserService;
import com.nc.sinpase.poc.modulith.covoit.identity.UserView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserService userService;
    private final RefreshSessionRepository sessionRepository;
    private final TokenIssuer tokenIssuer;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService,
                       RefreshSessionRepository sessionRepository,
                       TokenIssuer tokenIssuer,
                       PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.sessionRepository = sessionRepository;
        this.tokenIssuer = tokenIssuer;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthTokenView register(RegisterCommand command, HttpServletRequest request) {
        validatePhoneNumber(command.phoneDialCode(), command.phoneNumber());

        String passwordHash = passwordEncoder.encode(command.password());

        UserView user = userService.createUser(new CreateUserCommand(
                command.email(), passwordHash, command.displayName(),
                command.phoneDialCode(), command.phoneNumber()
        ));

        return issueTokens(user.id(), user.roles().stream().toList(),
                extractDeviceId(request), request.getHeader("User-Agent"), request.getRemoteAddr());
    }

    public AuthTokenView login(LoginCommand command, HttpServletRequest request) {
        UserCredentials credentials = userService.findCredentialsByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), credentials.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        userService.recordLogin(credentials.userId());

        return issueTokens(credentials.userId(), credentials.roles().stream().toList(),
                extractDeviceId(request), request.getHeader("User-Agent"), request.getRemoteAddr());
    }

    public AuthTokenView refresh(String rawRefreshToken, HttpServletRequest request) {
        String hash = hashToken(rawRefreshToken);
        RefreshSession session = sessionRepository.findByTokenHash(hash)
                .orElseThrow(InvalidTokenException::new);

        if (!session.isValid()) {
            throw new InvalidTokenException();
        }

        UserView user = userService.findById(session.getUserId())
                .orElseThrow(InvalidTokenException::new);

        String newRawToken = generateOpaqueToken();
        String newHash = hashToken(newRawToken);
        long expiresIn = tokenIssuer.getRefreshTokenExpirationSeconds();

        RefreshSession newSession = RefreshSession.create(
                user.id(), newHash, expiresIn,
                extractDeviceId(request), request.getHeader("User-Agent"), request.getRemoteAddr()
        );

        session.revokeAndReplace(newSession.getId());
        sessionRepository.save(session);
        sessionRepository.save(newSession);

        String accessToken = tokenIssuer.generateAccessToken(user.id(), user.roles().stream().toList());
        return new AuthTokenView(user.id(), accessToken, newRawToken, tokenIssuer.getAccessTokenExpirationSeconds());
    }

    public void logout(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        sessionRepository.findByTokenHash(hash).ifPresent(session -> {
            session.revoke();
            sessionRepository.save(session);
        });
    }

    public void logoutAll(UUID userId) {
        List<RefreshSession> sessions = sessionRepository.findActiveByUserId(userId);
        sessions.forEach(RefreshSession::revoke);
        sessionRepository.saveAll(sessions);
    }

    // --- Helpers ---

    private AuthTokenView issueTokens(UUID userId, List<String> roles,
                                      String deviceId, String userAgent, String ip) {
        String rawRefreshToken = generateOpaqueToken();
        String hash = hashToken(rawRefreshToken);
        long refreshExpiry = tokenIssuer.getRefreshTokenExpirationSeconds();

        RefreshSession session = RefreshSession.create(userId, hash, refreshExpiry, deviceId, userAgent, ip);
        sessionRepository.save(session);

        String accessToken = tokenIssuer.generateAccessToken(userId, roles);
        return new AuthTokenView(userId, accessToken, rawRefreshToken, tokenIssuer.getAccessTokenExpirationSeconds());
    }

    private void validatePhoneNumber(String dialCode, String number) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String regionCode = phoneUtil.getRegionCodeForCountryCode(
                Integer.parseInt(dialCode.replace("+", ""))
        );
        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(number, regionCode);
            if (!phoneUtil.isValidNumber(parsed)) {
                throw new IllegalArgumentException("Invalid phone number for dial code " + dialCode);
            }
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid phone number: " + e.getMessage());
        }
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        return deviceId != null ? deviceId : "unknown";
    }
}
