package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.JourneyPlannedActivityItemResponse;
import br.com.playercontabilidade.registroponto.dto.JourneyResponse;
import br.com.playercontabilidade.registroponto.dto.PlannedActivityRequest;
import br.com.playercontabilidade.registroponto.dto.UnplannedActivityResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.PlannedActivity;
import br.com.playercontabilidade.registroponto.entity.UnplannedActivity;
import br.com.playercontabilidade.registroponto.exception.ColaboratorNotFoundException;
import br.com.playercontabilidade.registroponto.exception.JourneyAlreadyInProgressException;
import br.com.playercontabilidade.registroponto.exception.JourneyNotFoundException;
import br.com.playercontabilidade.registroponto.exception.JourneyNotModifiableException;
import br.com.playercontabilidade.registroponto.exception.JourneyPlannedActivityNotFoundException;
import br.com.playercontabilidade.registroponto.exception.UnplannedActivityNotFoundException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyPlannedActivityRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import br.com.playercontabilidade.registroponto.repository.PlannedActivityRepository;
import br.com.playercontabilidade.registroponto.repository.UnplannedActivityRepository;
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
    private final UnplannedActivityRepository unplannedActivityRepository;
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

    @Transactional
    public UnplannedActivityResponse addUnplannedActivity(
            String username, Long journeyId, PlannedActivityRequest request) {
        Colaborator colaborator = resolveColaborator(username);
        Journey journey = journeyRepository
                .findByIdAndColaborator_Id(journeyId, colaborator.getId())
                .orElseThrow(() -> new JourneyNotFoundException(
                        "Jornada não encontrada ou não pertence ao colaborador autenticado."));
        assertJourneyAllowsUnplannedChanges(journey);

        UnplannedActivity entity = UnplannedActivity.builder()
                .journey(journey)
                .description(request.description().trim())
                .build();
        journey.getUnplannedActivities().add(entity);
        UnplannedActivity saved = unplannedActivityRepository.save(entity);
        return toUnplannedResponse(saved);
    }

    @Transactional
    public UnplannedActivityResponse deleteUnplannedActivity(String username, Long unplannedActivityId) {
        Colaborator colaborator = resolveColaborator(username);
        UnplannedActivity entity = unplannedActivityRepository
                .findByIdAndJourney_Colaborator_Id(unplannedActivityId, colaborator.getId())
                .orElseThrow(() -> new UnplannedActivityNotFoundException(
                        "Atividade não planejada não encontrada ou não pertence ao colaborador autenticado."));
        assertJourneyAllowsUnplannedChanges(entity.getJourney());
        UnplannedActivityResponse response = toUnplannedResponse(entity);
        unplannedActivityRepository.delete(entity);
        return response;
    }

    private void assertJourneyAllowsUnplannedChanges(Journey journey) {
        if (journey.getStatus() != JourneyStatus.IN_PROGRESS || journey.getEndedAt() != null) {
            throw new JourneyNotModifiableException(
                    "Só é possível incluir ou remover atividades não planejadas enquanto a jornada estiver em andamento.");
        }
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

        List<UnplannedActivityResponse> unplannedActivities = journey.getUnplannedActivities()
                .stream()
                .map(this::toUnplannedResponse)
                .toList();

        return new JourneyResponse(
                journey.getId(),
                journey.getColaborator().getId(),
                appTimeService.toOffsetDateTime(journey.getStartedAt()),
                plannedActivities,
                unplannedActivities,
                journey.getStatus(),
                appTimeService.toOffsetDateTime(journey.getCreatedAt()),
                appTimeService.toOffsetDateTime(journey.getUpdatedAt()));
    }

    private UnplannedActivityResponse toUnplannedResponse(UnplannedActivity entity) {
        return new UnplannedActivityResponse(
                entity.getId(),
                entity.getJourney().getId(),
                entity.getDescription(),
                appTimeService.toOffsetDateTime(entity.getCreatedAt()));
    }

    private JourneyPlannedActivityItemResponse toItemResponse(JourneyPlannedActivity item) {
        return new JourneyPlannedActivityItemResponse(
                item.getId(),
                item.getPlannedActivity().getId(),
                item.getDescription(),
                item.isChecked());
    }
}
