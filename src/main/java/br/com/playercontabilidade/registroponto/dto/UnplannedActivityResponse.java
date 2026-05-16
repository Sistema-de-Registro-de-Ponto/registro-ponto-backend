package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record UnplannedActivityResponse(
        Long id,
        @JsonProperty("journey_id") Long journeyId,
        String description,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
}
