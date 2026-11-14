package com.jobtracker.controller;

import com.jobtracker.dto.AuthLoginRequest;
import com.jobtracker.dto.AuthRegisterRequest;
import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.AuthUserResponse;
import com.jobtracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(r));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody AuthLoginRequest r) {
        return service.login(r);
    }

    @GetMapping("/me")
    AuthUserResponse me(Authentication a) {
        return service.me(a.getName());
    }
}
