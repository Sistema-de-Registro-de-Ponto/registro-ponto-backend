package br.com.playercontabilidade.registroponto.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JourneyStatus {

    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    private final String value;

    JourneyStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
