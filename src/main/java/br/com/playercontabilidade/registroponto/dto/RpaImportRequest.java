package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RpaImportRequest(
        @NotBlank(message = "source_system é obrigatório")
        @Size(max = 64, message = "source_system deve ter no máximo 64 caracteres")
        @JsonProperty("source_system")
        String sourceSystem,

        @NotEmpty(message = "records não pode ser vazio")
        @Valid
        List<RpaImportRecordRequest> records
) {
}
