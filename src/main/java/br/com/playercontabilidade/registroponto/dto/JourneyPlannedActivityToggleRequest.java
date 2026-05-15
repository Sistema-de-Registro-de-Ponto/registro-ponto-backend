package br.com.playercontabilidade.registroponto.dto;

import jakarta.validation.constraints.NotNull;

public record JourneyPlannedActivityToggleRequest(

        @NotNull(message = "checked é obrigatório")
        Boolean checked
) {
}
