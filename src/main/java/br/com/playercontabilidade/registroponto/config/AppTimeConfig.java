package br.com.playercontabilidade.registroponto.config;

import br.com.playercontabilidade.registroponto.service.AppTimeService;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
@EnableConfigurationProperties(AppTimeProperties.class)
public class AppTimeConfig {

    @Bean
    public AppTimeService appTimeService(AppTimeProperties properties) {
        return new AppTimeService(properties.zone());
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTimeZoneCustomizer(AppTimeProperties properties) {
        return builder -> builder.timeZone(TimeZone.getTimeZone(properties.zone()));
    }
}
