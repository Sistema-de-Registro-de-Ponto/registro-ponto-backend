package br.com.playercontabilidade.registroponto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rpa")
public record RpaSecurityProperties(String apiKey) {
}
