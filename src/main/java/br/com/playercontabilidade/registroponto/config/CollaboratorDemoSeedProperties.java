package br.com.playercontabilidade.registroponto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.seed.collaborator-demo")
public record CollaboratorDemoSeedProperties(
        String defaultPassword,
        List<CollaboratorDemoProfile> collaborators) {
}
