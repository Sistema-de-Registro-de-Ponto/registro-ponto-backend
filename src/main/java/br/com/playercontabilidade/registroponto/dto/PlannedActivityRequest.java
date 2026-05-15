package br.com.playercontabilidade.registroponto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlannedActivityRequest(

        @NotBlank(message = "description é obrigatório")
        @Size(max = 500, message = "description deve ter no máximo 500 caracteres")
        String description
) {
}
