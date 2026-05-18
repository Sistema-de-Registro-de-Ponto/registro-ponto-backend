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
class UnplannedActivityRepositoryTest {

    private static final Instant RANGE_START = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant RANGE_END_EXCLUSIVE = Instant.parse("2026-05-12T00:00:00Z");
    private static final Instant INSIDE_RANGE = Instant.parse("2026-05-10T14:00:00Z");
    private static final Instant OUTSIDE_RANGE = Instant.parse("2026-05-12T10:00:00Z");

    @Autowired
    private UnplannedActivityRepository unplannedActivityRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ColaboratorRepository colaboratorRepository;

    private Colaborator colaborator;

    @BeforeEach
    void setUp() {
        User user = RepositoryTestFixtures.saveUser(userRepository, "unplannedrepotest");
        colaborator = RepositoryTestFixtures.saveColaborator(colaboratorRepository, user);
    }

    @Test
    void countByJourneyStartedAtBetween_deveContarAtividadesImprevistasNoIntervalo() {
        Journey inside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);
        RepositoryTestFixtures.saveUnplannedActivity(journeyRepository, inside, "Imprevista 1");
        RepositoryTestFixtures.saveUnplannedActivity(journeyRepository, inside, "Imprevista 2");

        Journey outside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, OUTSIDE_RANGE.plusSeconds(1000));
        RepositoryTestFixtures.saveUnplannedActivity(journeyRepository, outside, "Fora do intervalo");

        long count = unplannedActivityRepository.countByJourneyStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countByJourneyStartedAtBetween_deveIgnorarJornadaForaDoIntervalo() {
        Journey outside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, OUTSIDE_RANGE.plusSeconds(1000));
        RepositoryTestFixtures.saveUnplannedActivity(journeyRepository, outside, "Imprevista fora");

        long count = unplannedActivityRepository.countByJourneyStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isZero();
    }

    @Test
    void countByJourneyStartedAtBetween_deveRetornarZeroQuandoNaoHaAtividades() {
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);

        long count = unplannedActivityRepository.countByJourneyStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isZero();
    }
}
