package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RpaRecordResponse(
        Long id,
        @JsonProperty("source_system")
        String sourceSystem,
        @JsonProperty("external_employee_id")
        String externalEmployeeId,
        @JsonProperty("employee_name")
        String employeeName,
        @JsonProperty("work_date")
        LocalDate workDate,
        @JsonProperty("check_in_at")
        OffsetDateTime checkInAt,
        @JsonProperty("check_out_at")
        OffsetDateTime checkOutAt,
        @JsonProperty("worked_seconds")
        Long workedSeconds,
        @JsonProperty("raw_payload")
        JsonNode rawPayload,
        @JsonProperty("imported_at")
        OffsetDateTime importedAt,
        @JsonProperty("collaborator_id")
        Long collaboratorId,
        @JsonProperty("collaborator_first_name")
        String collaboratorFirstName
) {
}
