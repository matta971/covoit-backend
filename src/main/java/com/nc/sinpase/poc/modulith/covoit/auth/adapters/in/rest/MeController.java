package com.nc.sinpase.poc.modulith.covoit.auth.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.CurrentUserProvider;
import com.nc.sinpase.poc.modulith.covoit.identity.UserService;
import com.nc.sinpase.poc.modulith.covoit.identity.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class MeController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    MeController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    UserView me() {
        return userService.findById(currentUserProvider.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
