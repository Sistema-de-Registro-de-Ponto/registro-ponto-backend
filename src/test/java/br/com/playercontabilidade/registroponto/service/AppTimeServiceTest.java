package br.com.playercontabilidade.registroponto.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AppTimeServiceTest {

    @Test
    void deveConverterInstantParaOffsetDateTimeNoFusoConfigurado() {
        AppTimeService appTime = new AppTimeService("America/Sao_Paulo");
        Instant instant = Instant.parse("2026-05-15T15:05:10.530391Z");

        OffsetDateTime result = appTime.toOffsetDateTime(instant);

        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(-3));
        assertThat(result.getHour()).isEqualTo(12);
        assertThat(result.getMinute()).isEqualTo(5);
    }
}
