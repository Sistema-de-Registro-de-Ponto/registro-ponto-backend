package br.com.playercontabilidade.registroponto.dto;

import org.springframework.data.domain.Page;

public record ManagerConsolidatedReportResponse(
        ManagerConsolidatedReportPeriod period,
        ManagerConsolidatedReportSummary summary,
        Page<ManagerConsolidatedReportCollaboratorItem> collaborators
) {
}
