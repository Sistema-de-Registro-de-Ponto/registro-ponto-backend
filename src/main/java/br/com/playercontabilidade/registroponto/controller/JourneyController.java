package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.JourneyResponse;
import br.com.playercontabilidade.registroponto.service.JourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/journeys")
@RequiredArgsConstructor
@Tag(name = "Jornadas", description = "Início e controle da jornada de trabalho")
public class JourneyController {

    private final JourneyService journeyService;

    @GetMapping("/current")
    @Operation(summary = "Retorna a jornada em andamento do colaborador autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public JourneyResponse getCurrent(Authentication authentication) {
        return journeyService.getCurrentInProgress(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inicia a jornada do colaborador autenticado (check-in)")
    @SecurityRequirement(name = "bearerAuth")
    public JourneyResponse start(Authentication authentication) {
        return journeyService.start(authentication.getName());
    }
}
