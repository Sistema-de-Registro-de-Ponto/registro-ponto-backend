package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.ColaboratorProfileResponse;
import br.com.playercontabilidade.registroponto.service.ColaboratorService;
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
@Tag(name = "Colaborador", description = "Dados do colaborador vinculado ao usuário autenticado")
public class ColaboratorController {

    private final ColaboratorService colaboratorService;

    @GetMapping("/collaborator")
    @Operation(
            summary = "Perfil do colaborador autenticado",
            description = "Retorna o identificador do usuário e o primeiro nome cadastrado na tabela colaborators."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ColaboratorProfileResponse getColaborator(Authentication authentication) {
        return colaboratorService.getProfileForCurrentUser(authentication.getName());
    }
}
