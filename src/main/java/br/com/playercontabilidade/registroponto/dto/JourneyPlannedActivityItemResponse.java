package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JourneyPlannedActivityItemResponse(
        Long id,
        @JsonProperty("planned_activity_id") Long plannedActivityId,
        String description,
        @JsonProperty("is_checked") boolean isChecked
) {
}
