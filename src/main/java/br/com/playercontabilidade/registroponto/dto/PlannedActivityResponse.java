package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record PlannedActivityResponse(
        Long id,
        String description,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
}
