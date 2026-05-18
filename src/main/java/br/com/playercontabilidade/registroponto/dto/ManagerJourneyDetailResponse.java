package br.com.playercontabilidade.registroponto.dto;

import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record ManagerJourneyDetailResponse(
        Long id,
        @JsonProperty("collaborator_id") Long collaboratorId,
        @JsonProperty("collaborator_first_name") String collaboratorFirstName,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("ended_at") OffsetDateTime endedAt,
        @JsonProperty("duration_seconds") Long durationSeconds,
        String summary,
        @JsonProperty("journey_planned_activities") List<JourneyPlannedActivityItemResponse> plannedActivities,
        @JsonProperty("unplanned_activities") List<UnplannedActivityResponse> unplannedActivities,
        JourneyStatus status,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {
}
