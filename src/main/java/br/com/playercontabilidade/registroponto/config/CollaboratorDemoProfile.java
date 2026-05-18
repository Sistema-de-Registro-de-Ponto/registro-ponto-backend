package br.com.playercontabilidade.registroponto.config;

import java.time.LocalDate;

public record CollaboratorDemoProfile(
        String username,
        String password,
        String firstName,
        LocalDate anchorDate,
        Integer journeyCount,
        boolean monthOnly) {

    public String resolvePassword(String defaultPassword) {
        if (password != null && !password.isBlank()) {
            return password;
        }
        return defaultPassword;
    }
}
