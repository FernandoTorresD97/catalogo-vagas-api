package dev.fernandotorres.catalogo.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginCandidateRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
