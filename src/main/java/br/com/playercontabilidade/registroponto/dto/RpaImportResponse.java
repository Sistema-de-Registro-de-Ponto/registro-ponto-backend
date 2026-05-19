package br.com.playercontabilidade.registroponto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RpaImportResponse(
        @JsonProperty("imported_count")
        int importedCount,
        List<Long> ids
) {
}
