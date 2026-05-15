package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.JourneyPlannedActivityItemResponse;
import br.com.playercontabilidade.registroponto.dto.JourneyResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.PlannedActivity;
import br.com.playercontabilidade.registroponto.exception.ColaboratorNotFoundException;
import br.com.playercontabilidade.registroponto.exception.JourneyAlreadyInProgressException;
import br.com.playercontabilidade.registroponto.exception.JourneyNotFoundException;
import br.com.playercontabilidade.registroponto.exception.JourneyPlannedActivityNotFoundException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyPlannedActivityRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import br.com.playercontabilidade.registroponto.repository.PlannedActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JourneyService {

    private final ColaboratorRepository colaboratorRepository;
    private final PlannedActivityRepository plannedActivityRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyPlannedActivityRepository journeyPlannedActivityRepository;
    private final AppTimeService appTimeService;

    @Transactional
    public JourneyResponse start(String username) {
        Colaborator colaborator = resolveColaborator(username);

        if (journeyRepository.existsByColaborator_IdAndEndedAtIsNullAndStatus(
                colaborator.getId(), JourneyStatus.IN_PROGRESS)) {
            throw new JourneyAlreadyInProgressException(
                    "Já existe uma jornada em andamento para este colaborador.");
        }

        Instant startedAt = Instant.now();
        Journey journey = Journey.builder()
                .colaborator(colaborator)
                .startedAt(startedAt)
                .status(JourneyStatus.IN_PROGRESS)
                .build();

        List<PlannedActivity> backlog = plannedActivityRepository
                .findByColaborator_IdOrderByCreatedAtAsc(colaborator.getId());
        for (PlannedActivity plannedActivity : backlog) {
            journey.getPlannedActivities().add(JourneyPlannedActivity.builder()
                    .journey(journey)
                    .plannedActivity(plannedActivity)
                    .description(plannedActivity.getDescription())
                    .checked(false)
                    .build());
        }

        Journey saved = journeyRepository.save(journey);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JourneyResponse getCurrentInProgress(String username) {
        Colaborator colaborator = resolveColaborator(username);
        Journey journey = journeyRepository
                .findByColaborator_IdAndEndedAtIsNullAndStatus(colaborator.getId(), JourneyStatus.IN_PROGRESS)
                .orElseThrow(() -> new JourneyNotFoundException(
                        "Não há jornada em andamento para este colaborador."));
        return toResponse(journey);
    }

    @Transactional
    public JourneyPlannedActivityItemResponse updateJourneyPlannedActivityChecked(
            String username,
            Long journeyPlannedActivityId,
            boolean checked) {
        Colaborator colaborator = resolveColaborator(username);
        JourneyPlannedActivity item = journeyPlannedActivityRepository
                .findByIdAndJourney_Colaborator_IdAndJourney_EndedAtIsNullAndJourney_Status(
                        journeyPlannedActivityId,
                        colaborator.getId(),
                        JourneyStatus.IN_PROGRESS)
                .orElseThrow(() -> new JourneyPlannedActivityNotFoundException(
                        "Atividade da jornada não encontrada ou a jornada não está em andamento."));

        item.setChecked(checked);
        JourneyPlannedActivity saved = journeyPlannedActivityRepository.save(item);
        return toItemResponse(saved);
    }

    private Colaborator resolveColaborator(String username) {
        return colaboratorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ColaboratorNotFoundException(
                        "Não existe colaborador associado ao usuário autenticado."));
    }

    private JourneyResponse toResponse(Journey journey) {
        List<JourneyPlannedActivityItemResponse> plannedActivities = journey.getPlannedActivities()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new JourneyResponse(
                journey.getId(),
                journey.getColaborator().getId(),
                appTimeService.toOffsetDateTime(journey.getStartedAt()),
                plannedActivities,
                journey.getStatus(),
                appTimeService.toOffsetDateTime(journey.getCreatedAt()),
                appTimeService.toOffsetDateTime(journey.getUpdatedAt()));
    }

    private JourneyPlannedActivityItemResponse toItemResponse(JourneyPlannedActivity item) {
        return new JourneyPlannedActivityItemResponse(
                item.getId(),
                item.getPlannedActivity().getId(),
                item.getDescription(),
                item.isChecked());
    }
}
