package edu.arep.secureapp.dto;

public record AuthResponse(
        String message,
        String token
) {
}
