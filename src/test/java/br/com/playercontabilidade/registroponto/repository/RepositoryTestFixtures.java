package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.UnplannedActivity;
import br.com.playercontabilidade.registroponto.entity.User;

import java.time.Instant;

final class RepositoryTestFixtures {

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$dummyBcryptHashValueForTesting1234567890";

    private RepositoryTestFixtures() {
    }

    static User saveUser(UserRepository userRepository, String username) {
        User user = User.builder()
                .username(username)
                .password(DUMMY_BCRYPT_HASH)
                .role(Role.COLLABORATOR)
                .build();
        return userRepository.saveAndFlush(user);
    }

    static Colaborator saveColaborator(ColaboratorRepository colaboratorRepository, User user) {
        Colaborator colaborator = Colaborator.builder()
                .user(user)
                .firstName("Colaborador Teste")
                .build();
        return colaboratorRepository.saveAndFlush(colaborator);
    }

    static Journey saveJourney(
            JourneyRepository journeyRepository,
            Colaborator colaborator,
            Instant startedAt,
            JourneyStatus status,
            Long durationSeconds,
            Instant endedAt) {
        Journey journey = Journey.builder()
                .colaborator(colaborator)
                .startedAt(startedAt)
                .status(status)
                .durationSeconds(durationSeconds)
                .endedAt(endedAt)
                .build();
        return journeyRepository.saveAndFlush(journey);
    }

    static JourneyPlannedActivity saveJourneyPlannedActivity(
            JourneyRepository journeyRepository,
            Journey journey,
            String description,
            boolean checked) {
        JourneyPlannedActivity activity = JourneyPlannedActivity.builder()
                .journey(journey)
                .description(description)
                .checked(checked)
                .build();
        journey.getPlannedActivities().add(activity);
        journeyRepository.saveAndFlush(journey);
        return activity;
    }

    static UnplannedActivity saveUnplannedActivity(
            JourneyRepository journeyRepository,
            Journey journey,
            String description) {
        UnplannedActivity activity = UnplannedActivity.builder()
                .journey(journey)
                .description(description)
                .build();
        journey.getUnplannedActivities().add(activity);
        journeyRepository.saveAndFlush(journey);
        return activity;
    }
}
