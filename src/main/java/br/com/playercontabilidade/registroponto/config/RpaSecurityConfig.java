package br.com.playercontabilidade.registroponto.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RpaSecurityProperties.class)
public class RpaSecurityConfig {
}
