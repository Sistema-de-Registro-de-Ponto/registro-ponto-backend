package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.RpaImportRequest;
import br.com.playercontabilidade.registroponto.dto.RpaImportResponse;
import br.com.playercontabilidade.registroponto.service.RpaImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rpa")
@RequiredArgsConstructor
@Tag(name = "RPA", description = "Importação de registros coletados por automação RPA")
public class RpaImportController {

    private final RpaImportService rpaImportService;

    @PostMapping("/imports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Importa registros do portal externo",
            description = """
                    Recebe um lote de batidas de ponto coletadas pelo robô RPA.
                    Autenticação via header X-Rpa-Api-Key (sem JWT).
                    """
    )
    public RpaImportResponse importRecords(@Valid @RequestBody RpaImportRequest request) {
        return rpaImportService.importRecords(request);
    }
}
