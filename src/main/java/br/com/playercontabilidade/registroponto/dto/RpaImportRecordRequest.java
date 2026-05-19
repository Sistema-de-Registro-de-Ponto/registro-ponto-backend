package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RpaImportRecordRequest(
        @Size(max = 120, message = "external_employee_id deve ter no máximo 120 caracteres")
        @JsonProperty("external_employee_id")
        String externalEmployeeId,

        @NotBlank(message = "employee_name é obrigatório")
        @Size(max = 200, message = "employee_name deve ter no máximo 200 caracteres")
        @JsonProperty("employee_name")
        String employeeName,

        @NotNull(message = "work_date é obrigatório")
        @JsonProperty("work_date")
        LocalDate workDate,

        @NotNull(message = "check_in_at é obrigatório")
        @JsonProperty("check_in_at")
        OffsetDateTime checkInAt,

        @JsonProperty("check_out_at")
        OffsetDateTime checkOutAt,

        @JsonProperty("worked_seconds")
        Long workedSeconds,

        @JsonProperty("raw_payload")
        JsonNode rawPayload
) {
}
