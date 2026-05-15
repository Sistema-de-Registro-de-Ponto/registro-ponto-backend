package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record JourneyPlannedActivityToggleRequest(

        @JsonProperty("is_checked")
        @NotNull(message = "is_checked é obrigatório")
        Boolean isChecked
) {
}
