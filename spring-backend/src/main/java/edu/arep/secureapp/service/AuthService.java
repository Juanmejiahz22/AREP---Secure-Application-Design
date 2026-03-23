package edu.arep.secureapp.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.arep.secureapp.dto.AuthResponse;
import edu.arep.secureapp.dto.LoginRequest;
import edu.arep.secureapp.dto.RegisterRequest;
import edu.arep.secureapp.model.UserAccount;
import edu.arep.secureapp.repository.UserAccountRepository;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentMap<String, String> activeTokens = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El usuario ya existe");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return "Usuario registrado correctamente";
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales invalidas");
        }

        String token = UUID.randomUUID().toString();
        activeTokens.put(token, user.getUsername());

        return new AuthResponse("Login exitoso", token);
    }

    public Optional<String> validateToken(String token) {
        return Optional.ofNullable(activeTokens.get(token));
    }
}
