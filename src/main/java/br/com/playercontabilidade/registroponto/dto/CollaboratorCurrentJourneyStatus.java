package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CollaboratorCurrentJourneyStatus {

    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    NONE("none");

    private final String value;

    CollaboratorCurrentJourneyStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
