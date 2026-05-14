package br.com.playercontabilidade.registroponto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Verificação de status da aplicação")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Retorna o status atual da API")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "registro-ponto-backend",
                "timestamp", OffsetDateTime.now().toString()
        );
    }
}
