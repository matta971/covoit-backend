package com.nc.sinpase.poc.modulith.covoit.auth.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.auth.AuthService;
import com.nc.sinpase.poc.modulith.covoit.auth.AuthTokenView;
import com.nc.sinpase.poc.modulith.covoit.CurrentUserProvider;
import com.nc.sinpase.poc.modulith.covoit.auth.LoginCommand;
import com.nc.sinpase.poc.modulith.covoit.auth.RegisterCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthTokenView register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(new RegisterCommand(
                request.email(), request.password(), request.displayName(),
                request.phoneDialCode(), request.phoneNumber()
        ), httpRequest);
    }

    @PostMapping("/login")
    AuthTokenView login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(new LoginCommand(request.email(), request.password()), httpRequest);
    }

    @PostMapping("/refresh")
    AuthTokenView refresh(@RequestBody @Valid RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), httpRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody @Valid LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logoutAll() {
        authService.logoutAll(currentUserProvider.getUserId());
    }
}
