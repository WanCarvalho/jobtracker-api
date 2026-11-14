package com.jobtracker.service;

import com.jobtracker.dto.AuthLoginRequest;
import com.jobtracker.dto.AuthRegisterRequest;
import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.AuthUserResponse;
import com.jobtracker.entity.User;
import com.jobtracker.enums.UserRole;
import com.jobtracker.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return userResponse(u);
    }

    @Transactional
    public AuthUserResponse updateProfileImage(String image, String email) {
        validateImage(image);
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        user.updateProfileImage(image);
        return userResponse(user);
    }

    private void validateImage(String image) {
        String format;
        if (image.startsWith("data:image/png;base64,")) {
            format = "png";
        } else if (image.startsWith("data:image/jpeg;base64,")) {
            format = "JPEG";
        } else {
            throw new IllegalArgumentException("Selecione uma imagem PNG ou JPEG.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(image.substring(image.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("O conteúdo da imagem é inválido.");
        }
        if (bytes.length == 0 || bytes.length > 512 * 1024) {
            throw new IllegalArgumentException("A imagem deve ter no máximo 512 KB.");
        }

        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Imagem inválida.");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!reader.getFormatName().equalsIgnoreCase(format)
                        || reader.getWidth(0) > 1024
                        || reader.getHeight(0) > 1024) {
                    throw new IllegalArgumentException(
                            "Use uma imagem PNG ou JPEG de até 1024 × 1024 pixels.");
                }
                reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem selecionada.");
        }
    }

    private AuthUserResponse userResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getProfileImage());
    }

    private AuthResponse response(User u) {
        return new AuthResponse(
                jwt.generate(u),
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole().name(),
                u.getProfileImage());
    }
}
