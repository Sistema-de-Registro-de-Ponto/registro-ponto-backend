package br.com.playercontabilidade.registroponto.dto;

import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record JourneyResponse(
        Long id,
        @JsonProperty("collaborator_id") Long collaboratorId,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("journey_planned_activities") List<JourneyPlannedActivityItemResponse> plannedActivities,
        @JsonProperty("unplanned_activities") List<UnplannedActivityResponse> unplannedActivities,
        JourneyStatus status,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {
}
