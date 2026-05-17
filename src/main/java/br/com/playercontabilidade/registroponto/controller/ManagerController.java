package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.ManagerProfileResponse;
import br.com.playercontabilidade.registroponto.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Gerente", description = "Dados do gerente vinculado ao usuário autenticado")
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping("/manager")
    @Operation(
            summary = "Perfil do gerente autenticado",
            description = "Retorna o identificador do usuário e o primeiro nome cadastrado na tabela managers."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ManagerProfileResponse getManager(Authentication authentication) {
        return managerService.getProfileForCurrentUser(authentication.getName());
    }
}
