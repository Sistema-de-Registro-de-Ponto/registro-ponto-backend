package br.com.playercontabilidade.registroponto.config;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.UnplannedActivity;
import br.com.playercontabilidade.registroponto.entity.User;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import br.com.playercontabilidade.registroponto.repository.UserRepository;
import br.com.playercontabilidade.registroponto.service.AppTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed.collaborator-demo", name = "enabled", havingValue = "true")
public class CollaboratorDemoDataSeeder implements CommandLineRunner {

    private static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(17, 0);
    private static final int DEFAULT_JOURNEY_COUNT = 60;

    private final CollaboratorDemoSeedProperties properties;
    private final UserRepository userRepository;
    private final ColaboratorRepository colaboratorRepository;
    private final JourneyRepository journeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppTimeService appTimeService;

    @Override
    @Transactional
    public void run(String... args) {
        for (CollaboratorDemoProfile profile : properties.collaborators()) {
            seedCollaborator(profile);
        }
    }

    private void seedCollaborator(CollaboratorDemoProfile profile) {
        List<LocalDate> weekdays = resolveWeekdays(profile);
        int journeyCount = weekdays.size();
        Colaborator colaborator = resolveColaborator(profile);

        long existing = journeyRepository.countByColaborator_IdAndStatus(
                colaborator.getId(), JourneyStatus.COMPLETED);
        if (existing >= journeyCount) {
            log.info(
                    "Seed collaborator-demo: {} jornadas completed já existem para '{}'; ignorando.",
                    existing,
                    profile.username());
            return;
        }

        List<JourneyActivitySpec> specs = buildActivitySpecs(journeyCount);
        if (weekdays.size() != specs.size()) {
            throw new IllegalStateException(
                    "Seed collaborator-demo: quantidade de datas (%d) difere dos perfis (%d) para '%s'."
                            .formatted(weekdays.size(), specs.size(), profile.username()));
        }

        ZoneId zone = appTimeService.zone();
        List<Journey> journeys = new ArrayList<>(journeyCount);
        for (int i = 0; i < journeyCount; i++) {
            journeys.add(buildJourney(colaborator, weekdays.get(i), specs.get(i), zone));
        }
        journeyRepository.saveAll(journeys);
        log.info(
                "Seed collaborator-demo: criou {} jornadas completed para '{}'.",
                journeyCount,
                profile.username());
    }

    private List<LocalDate> resolveWeekdays(CollaboratorDemoProfile profile) {
        LocalDate anchor = profile.anchorDate() != null
                ? profile.anchorDate()
                : appTimeService.now().toLocalDate();
        if (profile.monthOnly()) {
            return collectWeekdaysFromTo(anchor.withDayOfMonth(1), anchor);
        }
        int count = profile.journeyCount() != null ? profile.journeyCount() : DEFAULT_JOURNEY_COUNT;
        return collectWeekdaysEndingOn(anchor, count);
    }

    private Colaborator resolveColaborator(CollaboratorDemoProfile profile) {
        String username = profile.username();
        String password = profile.resolvePassword(properties.defaultPassword());
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User created = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(password))
                            .role(Role.COLLABORATOR)
                            .build();
                    User saved = userRepository.save(created);
                    log.info("Seed collaborator-demo: criou usuário '{}'.", username);
                    return saved;
                });

        return colaboratorRepository.findByUser_Username(username)
                .orElseGet(() -> {
                    Colaborator created = Colaborator.builder()
                            .user(user)
                            .firstName(profile.firstName())
                            .build();
                    Colaborator saved = colaboratorRepository.save(created);
                    log.info("Seed collaborator-demo: criou colaborador para '{}'.", username);
                    return saved;
                });
    }

    private static List<LocalDate> collectWeekdaysEndingOn(LocalDate endInclusive, int count) {
        List<LocalDate> dates = new ArrayList<>(count);
        LocalDate cursor = endInclusive;
        while (dates.size() < count) {
            DayOfWeek day = cursor.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                dates.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }
        return dates;
    }

    private static List<LocalDate> collectWeekdaysFromTo(LocalDate startInclusive, LocalDate endInclusive) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = startInclusive;
        while (!cursor.isAfter(endInclusive)) {
            DayOfWeek day = cursor.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private static List<JourneyActivitySpec> buildActivitySpecs(int journeyCount) {
        if (journeyCount == 60) {
            List<JourneyActivitySpec> specs = new ArrayList<>(journeyCount);
            repeat(specs, 10, JourneyActivitySpec.of(10, 10, 0, 0));
            repeat(specs, 10, JourneyActivitySpec.of(10, 10, 0, 0));
            repeat(specs, 10, JourneyActivitySpec.of(15, 15, 0, 5));
            repeat(specs, 10, JourneyActivitySpec.of(10, 0, 10, 0));
            repeat(specs, 10, JourneyActivitySpec.of(5, 0, 5, 5));
            repeat(specs, 10, JourneyActivitySpec.of(10, 5, 5, 5));
            return specs;
        }
        List<JourneyActivitySpec> templates = List.of(
                JourneyActivitySpec.of(10, 10, 0, 0),
                JourneyActivitySpec.of(15, 15, 0, 5),
                JourneyActivitySpec.of(10, 0, 10, 0),
                JourneyActivitySpec.of(5, 0, 5, 5),
                JourneyActivitySpec.of(10, 5, 5, 5));
        List<JourneyActivitySpec> specs = new ArrayList<>(journeyCount);
        for (int i = 0; i < journeyCount; i++) {
            specs.add(templates.get(i % templates.size()));
        }
        return specs;
    }

    private static void repeat(List<JourneyActivitySpec> specs, int times, JourneyActivitySpec spec) {
        for (int i = 0; i < times; i++) {
            specs.add(spec);
        }
    }

    private Journey buildJourney(
            Colaborator colaborator,
            LocalDate date,
            JourneyActivitySpec spec,
            ZoneId zone) {
        Instant startedAt = date.atTime(WORKDAY_START).atZone(zone).toInstant();
        Instant endedAt = date.atTime(WORKDAY_END).atZone(zone).toInstant();
        long durationSeconds = Duration.between(startedAt, endedAt).getSeconds();

        Journey journey = Journey.builder()
                .colaborator(colaborator)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSeconds(durationSeconds)
                .status(JourneyStatus.COMPLETED)
                .build();

        long snapshotId = 1;
        for (int i = 0; i < spec.plannedChecked(); i++) {
            journey.getPlannedActivities().add(plannedActivity(journey, snapshotId++, true));
        }
        for (int i = 0; i < spec.plannedUnchecked(); i++) {
            journey.getPlannedActivities().add(plannedActivity(journey, snapshotId++, false));
        }
        for (int i = 0; i < spec.unplanned(); i++) {
            journey.getUnplannedActivities().add(unplannedActivity(journey, i + 1));
        }
        return journey;
    }

    private static JourneyPlannedActivity plannedActivity(Journey journey, long snapshotId, boolean checked) {
        return JourneyPlannedActivity.builder()
                .journey(journey)
                .plannedActivity(null)
                .snapshotPlannedActivityId(snapshotId)
                .description("Atividade planejada %d".formatted(snapshotId))
                .checked(checked)
                .build();
    }

    private static UnplannedActivity unplannedActivity(Journey journey, int index) {
        return UnplannedActivity.builder()
                .journey(journey)
                .description("Atividade não planejada %d".formatted(index))
                .build();
    }

    private record JourneyActivitySpec(int plannedChecked, int plannedUnchecked, int unplanned) {

        static JourneyActivitySpec of(
                int plannedTotal,
                int plannedChecked,
                int plannedUnchecked,
                int unplanned) {
            if (plannedChecked + plannedUnchecked != plannedTotal) {
                throw new IllegalArgumentException("plannedChecked + plannedUnchecked deve ser igual a plannedTotal");
            }
            return new JourneyActivitySpec(plannedChecked, plannedUnchecked, unplanned);
        }
    }
}
