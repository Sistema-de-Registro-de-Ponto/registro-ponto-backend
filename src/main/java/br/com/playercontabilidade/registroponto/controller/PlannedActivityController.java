package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.PlannedActivityRequest;
import br.com.playercontabilidade.registroponto.dto.PlannedActivityResponse;
import br.com.playercontabilidade.registroponto.service.PlannedActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/activities/planned")
@RequiredArgsConstructor
@Tag(name = "Atividades planejadas", description = "Atividades informadas antes de iniciar a jornada")
public class PlannedActivityController {

    private final PlannedActivityService plannedActivityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma atividade planejada para o colaborador autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public PlannedActivityResponse create(
            Authentication authentication,
            @Valid @RequestBody PlannedActivityRequest request) {
        return plannedActivityService.create(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "Lista as atividades planejadas do colaborador autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public List<PlannedActivityResponse> list(Authentication authentication) {
        return plannedActivityService.listForCurrentUser(authentication.getName());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma atividade planejada do colaborador autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public PlannedActivityResponse delete(
            Authentication authentication,
            @PathVariable Long id) {
        return plannedActivityService.delete(authentication.getName(), id);
    }
}
