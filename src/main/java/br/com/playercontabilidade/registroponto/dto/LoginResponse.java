package br.com.playercontabilidade.registroponto.dto;

import br.com.playercontabilidade.registroponto.entity.Role;

public record LoginResponse(String token, String tokenType, Role role) {

    public static LoginResponse bearer(String token, Role role) {
        return new LoginResponse(token, "Bearer", role);
    }
}
