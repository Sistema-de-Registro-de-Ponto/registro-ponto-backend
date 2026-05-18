package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class JourneyPlannedActivityRepositoryTest {

    private static final Instant RANGE_START = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant RANGE_END_EXCLUSIVE = Instant.parse("2026-05-12T00:00:00Z");
    private static final Instant INSIDE_RANGE = Instant.parse("2026-05-10T14:00:00Z");
    private static final Instant OUTSIDE_RANGE = Instant.parse("2026-05-12T10:00:00Z");

    @Autowired
    private JourneyPlannedActivityRepository journeyPlannedActivityRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ColaboratorRepository colaboratorRepository;

    private Colaborator colaborator;

    @BeforeEach
    void setUp() {
        User user = RepositoryTestFixtures.saveUser(userRepository, "jparepotest");
        colaborator = RepositoryTestFixtures.saveColaborator(colaboratorRepository, user);
    }

    @Test
    void countCheckedByJourneyStartedAtBetween_deveContarApenasAtividadesMarcadasNoIntervalo() {
        Journey inside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Concluída 1", true);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Concluída 2", true);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Pendente", false);

        Journey outside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, OUTSIDE_RANGE.plusSeconds(1000));
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, outside, "Fora do intervalo", true);

        long count = journeyPlannedActivityRepository.countCheckedByJourneyStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countCheckedByJourneyStartedAtBetween_deveIgnorarJornadaForaDoIntervaloMesmoComAtividadeMarcada() {
        Journey outside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, OUTSIDE_RANGE.plusSeconds(1000));
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, outside, "Concluída fora", true);

        long count = journeyPlannedActivityRepository.countCheckedByJourneyStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isZero();
    }

    @Test
    void countCheckedByJourneyStartedAtBetween_deveRetornarZeroQuandoNaoHaAtividadesMarcadas() {
        Journey inside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Pendente 1", false);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Pendente 2", false);

        long count = journeyPlannedActivityRepository.countCheckedByJourneyStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isZero();
    }
}
