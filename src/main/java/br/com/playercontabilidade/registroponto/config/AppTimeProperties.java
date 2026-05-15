package br.com.playercontabilidade.registroponto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.time")
public record AppTimeProperties(String zone) {
}
