package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.JourneyPlannedActivityItemResponse;
import br.com.playercontabilidade.registroponto.dto.JourneyPlannedActivityToggleRequest;
import br.com.playercontabilidade.registroponto.service.JourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/journeys/activities/planned")
@RequiredArgsConstructor
@Tag(name = "Atividades planejadas na jornada", description = "Marca ou desmarca itens vinculados à jornada em andamento")
public class JourneyPlannedActivityController {

    private final JourneyService journeyService;

    @PutMapping("/{id}")
    @Operation(summary = "Marca ou desmarca uma atividade planejada na jornada atual")
    @SecurityRequirement(name = "bearerAuth")
    public JourneyPlannedActivityItemResponse toggleChecked(
            Authentication authentication,
            @Parameter(description = "Identificador do vínculo na jornada (campo id em journey_planned_activities)")
            @PathVariable("id") Long id,
            @Valid @RequestBody JourneyPlannedActivityToggleRequest request) {
        return journeyService.updateJourneyPlannedActivityChecked(
                authentication.getName(), id, request.isChecked());
    }
}
