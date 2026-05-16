package br.com.playercontabilidade.registroponto.dto;

import jakarta.validation.constraints.Size;

public record JourneyEndRequest(

        @Size(max = 2000, message = "summary deve ter no máximo 2000 caracteres")
        String summary
) {
}
