package edu.arep.secureapp.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.arep.secureapp.service.AuthService;

@RestController
@RequestMapping("/api/secure")
public class SecureController {
    private final AuthService authService;

    public SecureController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      Authentication authentication) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token requerido"));
        }

        String token = authorization.substring(7);
        return authService.validateToken(token)
                .map(username -> ResponseEntity.ok(Map.of(
                        "message", "Conexion segura y autenticada",
                        "user", username
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token invalido")));
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("message", "Pong - CORS funciona"));
    }
}
