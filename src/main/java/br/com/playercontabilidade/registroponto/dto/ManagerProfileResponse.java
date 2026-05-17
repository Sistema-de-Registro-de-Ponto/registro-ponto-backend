package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManagerProfileResponse(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("first_name") String firstName
) {
}
