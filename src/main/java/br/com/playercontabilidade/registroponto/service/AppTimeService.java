package br.com.playercontabilidade.registroponto.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Ponto único para conversão e obtenção de data/hora na aplicação.
 * O fuso é definido em {@code app.time.zone} (padrão: America/Sao_Paulo).
 */
public class AppTimeService {

    private final ZoneId zone;

    public AppTimeService(String zoneId) {
        this.zone = ZoneId.of(zoneId);
    }

    public ZoneId zone() {
        return zone;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(zone);
    }

    public OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atZone(zone).toOffsetDateTime();
    }
}
