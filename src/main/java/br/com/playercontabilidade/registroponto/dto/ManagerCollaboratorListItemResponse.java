package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManagerCollaboratorListItemResponse(
        Long id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("current_journey_status") CollaboratorCurrentJourneyStatus currentJourneyStatus,
        @JsonProperty("hours_today_seconds") long hoursTodaySeconds,
        @JsonProperty("adherence_percentage") Integer adherencePercentage
) {
}
