package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.ManagerOverviewResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerProfileResponse;
import br.com.playercontabilidade.registroponto.service.ManagerOverviewService;
import br.com.playercontabilidade.registroponto.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Gerente", description = "Dados do gerente vinculado ao usuário autenticado")
public class ManagerController {

    private final ManagerService managerService;
    private final ManagerOverviewService managerOverviewService;

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
}
