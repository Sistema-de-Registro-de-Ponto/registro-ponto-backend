package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.JourneyEndRequest;
import br.com.playercontabilidade.registroponto.dto.JourneyResponse;
import br.com.playercontabilidade.registroponto.service.JourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/journeys")
@RequiredArgsConstructor
@Validated
@Tag(name = "Jornadas", description = "Check-in, check-out e consulta da jornada")
public class JourneyController {

    private final JourneyService journeyService;

    @GetMapping({"", "/"})
    @Operation(summary = "Lista o histórico de jornadas do colaborador autenticado no período informado")
    @SecurityRequirement(name = "bearerAuth")
    public Slice<JourneyResponse> list(
            Authentication authentication,
            @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) @Min(1) Integer size) {
        int pageSize = size != null ? size : JourneyService.DEFAULT_PAGE_SIZE;
        return journeyService.list(authentication.getName(), startDate, endDate, page, pageSize);
    }

    @GetMapping("/current")
    @Operation(summary = "Retorna a jornada em andamento do colaborador autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public JourneyResponse getCurrent(Authentication authentication) {
        return journeyService.getCurrentInProgress(authentication.getName());
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inicia a jornada do colaborador autenticado (check-in)")
    @SecurityRequirement(name = "bearerAuth")
    public JourneyResponse start(Authentication authentication) {
        return journeyService.start(authentication.getName());
    }

    @PostMapping("/current/end")
    @Operation(summary = "Encerra a jornada em andamento (check-out)")
    @SecurityRequirement(name = "bearerAuth")
    public JourneyResponse end(
            Authentication authentication,
            @Valid @RequestBody JourneyEndRequest request) {
        return journeyService.end(authentication.getName(), request);
    }
}
