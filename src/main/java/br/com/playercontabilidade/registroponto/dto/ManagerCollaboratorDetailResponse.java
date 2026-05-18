package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManagerCollaboratorDetailResponse(
        Long id,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("hours_today_seconds") long hoursTodaySeconds,
        @JsonProperty("adherence_percentage") Integer adherencePercentage,
        @JsonProperty("current_journey") JourneyResponse currentJourney
) {
}
