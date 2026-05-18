package br.com.playercontabilidade.registroponto.dto;

import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ManagerJourneyListItemResponse(
        Long id,
        @JsonProperty("journey_date") LocalDate journeyDate,
        @JsonProperty("collaborator_id") Long collaboratorId,
        @JsonProperty("collaborator_first_name") String collaboratorFirstName,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("ended_at") OffsetDateTime endedAt,
        @JsonProperty("duration_seconds") long durationSeconds,
        JourneyStatus status
) {
}
