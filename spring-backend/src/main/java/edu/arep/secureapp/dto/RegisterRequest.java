package edu.arep.secureapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 4, max = 40) String username,
        @NotBlank @Size(min = 8, max = 80) String password
) {
}
