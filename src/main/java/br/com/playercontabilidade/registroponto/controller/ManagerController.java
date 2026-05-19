package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.ManagerCollaboratorDetailResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerCollaboratorListItemResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerConsolidatedReportResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerJourneyDetailResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerJourneyListItemResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerOverviewResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerProfileResponse;
import br.com.playercontabilidade.registroponto.dto.RpaRecordResponse;
import br.com.playercontabilidade.registroponto.service.JourneyService;
import br.com.playercontabilidade.registroponto.service.ManagerCollaboratorsService;
import br.com.playercontabilidade.registroponto.service.ManagerConsolidatedReportService;
import br.com.playercontabilidade.registroponto.service.ManagerJourneysService;
import br.com.playercontabilidade.registroponto.service.ManagerOverviewService;
import br.com.playercontabilidade.registroponto.service.ManagerRpaRecordsService;
import br.com.playercontabilidade.registroponto.service.ManagerService;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Gerente", description = "Dados do gerente vinculado ao usuário autenticado")
public class ManagerController {

    private final ManagerService managerService;
    private final ManagerOverviewService managerOverviewService;
    private final ManagerCollaboratorsService managerCollaboratorsService;
    private final ManagerJourneysService managerJourneysService;
    private final ManagerConsolidatedReportService managerConsolidatedReportService;
    private final ManagerRpaRecordsService managerRpaRecordsService;

    @GetMapping("/manager")
    @Operation(
            summary = "Perfil do gerente autenticado",
            description = "Retorna o identificador do usuário e o primeiro nome cadastrado na tabela managers."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ManagerProfileResponse getManager(Authentication authentication) {
        return managerService.getProfileForCurrentUser(authentication.getName());
    }

    @GetMapping("/manager/overview")
    @Operation(
            summary = "Indicadores gerais do dashboard",
            description = """
                    Retorna métricas agregadas de todas as jornadas no período informado.
                    Sem parâmetros de data, utiliza o dia atual no fuso configurado da aplicação.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public ManagerOverviewResponse getOverview(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return managerOverviewService.getOverview(startDate, endDate);
    }

    @GetMapping("/manager/collaborators")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Lista colaboradores da equipe",
            description = """
                    Retorna colaboradores cadastrados (role COLLABORATOR) com métricas do dia atual.
                    Aderência calculada apenas para jornada em andamento.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public Page<ManagerCollaboratorListItemResponse> listCollaborators(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) @Min(1) Integer size) {
        int pageSize = size != null ? size : JourneyService.DEFAULT_PAGE_SIZE;
        return managerCollaboratorsService.list(search, page, pageSize);
    }

    @GetMapping("/manager/collaborators/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Detalhe do colaborador",
            description = """
                    Retorna dados do colaborador e a jornada em andamento (se houver).
                    Aderência calculada apenas para jornada em andamento.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public ManagerCollaboratorDetailResponse getCollaborator(@PathVariable Long id) {
        return managerCollaboratorsService.getById(id);
    }

    @GetMapping("/manager/journeys")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Lista jornadas registradas",
            description = """
                    Retorna jornadas de colaboradores no período informado.
                    Sem datas, utiliza o dia atual no fuso da aplicação.
                    Filtro opcional por nome do colaborador (contains, case-insensitive).
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public Page<ManagerJourneyListItemResponse> listJourneys(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "collaborator_name", required = false) String collaboratorName,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) @Min(1) Integer size) {
        int pageSize = size != null ? size : JourneyService.DEFAULT_PAGE_SIZE;
        return managerJourneysService.list(startDate, endDate, collaboratorName, page, pageSize);
    }

    @GetMapping("/manager/reports/consolidated")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Relatório consolidado da operação",
            description = """
                    Retorna indicadores globais e tabela paginada por colaborador no período.
                    Apenas colaboradores com ao menos uma jornada no intervalo.
                    Horas consideram somente jornadas finalizadas (completed).
                    Summary e tabela respeitam o filtro search.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public ManagerConsolidatedReportResponse getConsolidatedReport(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) @Min(1) Integer size) {
        int pageSize = size != null ? size : JourneyService.DEFAULT_PAGE_SIZE;
        return managerConsolidatedReportService.getReport(startDate, endDate, search, page, pageSize);
    }

    @GetMapping("/manager/journeys/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Detalhe da jornada",
            description = """
                    Retorna a jornada completa com atividades planejadas e não planejadas.
                    Em andamento, ended_at, duration_seconds e summary vêm como null.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public ManagerJourneyDetailResponse getJourney(@PathVariable Long id) {
        return managerJourneysService.getById(id);
    }

    @GetMapping("/manager/rpa/records")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Lista registros importados via RPA",
            description = """
                    Retorna batidas de ponto espelhadas do portal externo (Ponto Ágil ou mock).
                    Sem datas, utiliza o dia atual no fuso da aplicação.
                    Filtro opcional por nome do colaborador no portal (contains, case-insensitive).
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    public Page<RpaRecordResponse> listRpaRecords(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) @Min(1) Integer size) {
        int pageSize = size != null ? size : JourneyService.DEFAULT_PAGE_SIZE;
        return managerRpaRecordsService.list(startDate, endDate, search, page, pageSize);
    }
}
