package com.jobtracker.service;

import com.jobtracker.dto.AuthLoginRequest;
import com.jobtracker.dto.AuthRegisterRequest;
import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.AuthUserResponse;
import com.jobtracker.entity.User;
import com.jobtracker.enums.UserRole;
import com.jobtracker.repository.UserRepository;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager auth;
    private final JwtService jwt;

    public AuthService(
            UserRepository users,
            PasswordEncoder encoder,
            AuthenticationManager auth,
            JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.auth = auth;
        this.jwt = jwt;
    }

    public AuthResponse register(AuthRegisterRequest r) {
        if (users.findByEmailIgnoreCase(r.email()).isPresent())
            throw new IllegalStateException("E-mail já cadastrado.");
        User u =
                users.save(
                        new User(
                                r.name(),
                                r.email().toLowerCase(),
                                encoder.encode(r.password()),
                                UserRole.USER));
        return response(u);
    }

    public AuthResponse login(AuthLoginRequest r) {
        auth.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        return response(users.findByEmailIgnoreCase(r.email()).orElseThrow());
    }

    public AuthUserResponse me(String email) {
        var u = users.findByEmailIgnoreCase(email).orElseThrow();
        return new AuthUserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }

    private AuthResponse response(User u) {
        return new AuthResponse(
                jwt.generate(u), u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
}
