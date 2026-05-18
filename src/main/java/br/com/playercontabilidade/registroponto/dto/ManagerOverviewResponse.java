package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManagerOverviewResponse(
        @JsonProperty("duration_seconds") long durationSeconds,
        @JsonProperty("journeys_progress") long journeysProgress,
        @JsonProperty("average_adherence_percentage") int averageAdherencePercentage,
        @JsonProperty("activities_completed") long activitiesCompleted,
        @JsonProperty("unplanned_activities") long unplannedActivities
) {
}
