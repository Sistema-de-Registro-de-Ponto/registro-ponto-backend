package br.com.playercontabilidade.registroponto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "app.seed.collaborator-demo")
public record CollaboratorDemoSeedProperties(
        String username,
        String password,
        String firstName,
        LocalDate anchorDate,
        int journeyCount) {
}
