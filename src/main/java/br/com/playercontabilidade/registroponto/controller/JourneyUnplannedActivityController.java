package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.PlannedActivityRequest;
import br.com.playercontabilidade.registroponto.dto.UnplannedActivityResponse;
import br.com.playercontabilidade.registroponto.service.JourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/journeys")
@RequiredArgsConstructor
@Tag(name = "Atividades não planejadas na jornada", description = "Registro de atividades realizadas fora do planejamento, durante a jornada em andamento")
public class JourneyUnplannedActivityController {

    private final JourneyService journeyService;

    @PostMapping({"/{journeyId}/activities/unplanned", "/{journeyId}/activities/unplanned/"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona uma atividade não planejada à jornada indicada")
    @SecurityRequirement(name = "bearerAuth")
    public UnplannedActivityResponse create(
            Authentication authentication,
            @PathVariable("journeyId") Long journeyId,
            @Valid @RequestBody PlannedActivityRequest request) {
        return journeyService.addUnplannedActivity(authentication.getName(), journeyId, request);
    }

    @DeleteMapping("/activities/unplanned/{id}")
    @Operation(summary = "Remove uma atividade não planejada da jornada em andamento")
    @SecurityRequirement(name = "bearerAuth")
    public UnplannedActivityResponse delete(
            Authentication authentication,
            @PathVariable("id") Long id) {
        return journeyService.deleteUnplannedActivity(authentication.getName(), id);
    }
}
