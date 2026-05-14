package br.com.playercontabilidade.registroponto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]+$", message = "Username deve conter apenas letras")
        String username,

        @NotBlank
        @Pattern(regexp = "^\\d{8}$", message = "Senha deve conter exatamente 8 dígitos numéricos")
        String password
) {
}
