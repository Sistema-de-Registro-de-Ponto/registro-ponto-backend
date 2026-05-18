package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManagerConsolidatedReportSummary(
        @JsonProperty("duration_seconds") long durationSeconds,
        @JsonProperty("planned_activities") long plannedActivities,
        @JsonProperty("activities_completed") long activitiesCompleted,
        @JsonProperty("unplanned_activities") long unplannedActivities,
        @JsonProperty("average_adherence_percentage") int averageAdherencePercentage
) {
}
