package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class JourneyRepositoryTest {

    private static final Instant RANGE_START = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant RANGE_END_EXCLUSIVE = Instant.parse("2026-05-12T00:00:00Z");
    private static final Instant INSIDE_RANGE = Instant.parse("2026-05-10T14:00:00Z");
    private static final Instant OUTSIDE_RANGE = Instant.parse("2026-05-12T10:00:00Z");

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ColaboratorRepository colaboratorRepository;

    private Colaborator colaborator;

    @BeforeEach
    void setUp() {
        User user = RepositoryTestFixtures.saveUser(userRepository, "journeyrepotest");
        colaborator = RepositoryTestFixtures.saveColaborator(colaboratorRepository, user);
    }

    @Test
    void sumDurationSecondsByStartedAtBetween_deveSomarApenasJornadasConcluidasNoIntervalo() {
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.COMPLETED, 3600L, INSIDE_RANGE.plusSeconds(3600));
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE.plusSeconds(60),
                JourneyStatus.COMPLETED, 1800L, INSIDE_RANGE.plusSeconds(1860));
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE.plusSeconds(120),
                JourneyStatus.IN_PROGRESS, 900L, null);
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 5000L, OUTSIDE_RANGE.plusSeconds(5000));

        long total = journeyRepository.sumDurationSecondsByStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(total).isEqualTo(5400L);
    }

    @Test
    void sumDurationSecondsByStartedAtBetween_deveRetornarZeroQuandoNaoHaJornadasConcluidas() {
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);

        long total = journeyRepository.sumDurationSecondsByStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(total).isZero();
    }

    @Test
    void countInProgressByStartedAtBetween_deveContarApenasJornadasEmAndamentoNoIntervalo() {
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE.plusSeconds(60),
                JourneyStatus.IN_PROGRESS, null, null);
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE.plusSeconds(120),
                JourneyStatus.COMPLETED, 1000L, INSIDE_RANGE.plusSeconds(1120));
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);

        long count = journeyRepository.countInProgressByStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countInProgressByStartedAtBetween_deveRetornarZeroQuandoNaoHaJornadasEmAndamento() {
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, INSIDE_RANGE.plusSeconds(1000));

        long count = journeyRepository.countInProgressByStartedAtBetween(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(count).isZero();
    }

    @Test
    void findByStartedAtBetweenWithPlannedActivities_deveRetornarJornadasDoIntervaloComAtividades() {
        Journey inside = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, INSIDE_RANGE.plusSeconds(1000));
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Atividade A", true);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, inside, "Atividade B", false);

        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 2000L, OUTSIDE_RANGE.plusSeconds(2000));

        List<Journey> journeys = journeyRepository.findByStartedAtBetweenWithPlannedActivities(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(journeys).hasSize(1);
        assertThat(journeys.getFirst().getId()).isEqualTo(inside.getId());
        assertThat(journeys.getFirst().getPlannedActivities()).hasSize(2);
        assertThat(journeys.getFirst().getPlannedActivities())
                .extracting(JourneyPlannedActivity::getDescription)
                .containsExactly("Atividade A", "Atividade B");
    }

    @Test
    void findByStartedAtBetweenWithPlannedActivities_naoDeveDuplicarJornadaComVariasAtividades() {
        Journey journey = RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, INSIDE_RANGE,
                JourneyStatus.IN_PROGRESS, null, null);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, journey, "Atividade 1", false);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, journey, "Atividade 2", false);
        RepositoryTestFixtures.saveJourneyPlannedActivity(
                journeyRepository, journey, "Atividade 3", false);

        List<Journey> journeys = journeyRepository.findByStartedAtBetweenWithPlannedActivities(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(journeys).hasSize(1);
        assertThat(journeys.getFirst().getPlannedActivities()).hasSize(3);
    }

    @Test
    void findByStartedAtBetweenWithPlannedActivities_deveRetornarVazioQuandoNaoHaJornadasNoIntervalo() {
        RepositoryTestFixtures.saveJourney(
                journeyRepository, colaborator, OUTSIDE_RANGE,
                JourneyStatus.COMPLETED, 1000L, OUTSIDE_RANGE.plusSeconds(1000));

        List<Journey> journeys = journeyRepository.findByStartedAtBetweenWithPlannedActivities(
                RANGE_START, RANGE_END_EXCLUSIVE);

        assertThat(journeys).isEmpty();
    }
}
