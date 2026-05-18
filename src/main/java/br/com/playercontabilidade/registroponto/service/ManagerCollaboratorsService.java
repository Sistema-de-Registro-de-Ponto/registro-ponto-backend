package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.CollaboratorCurrentJourneyStatus;
import br.com.playercontabilidade.registroponto.dto.JourneyResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerCollaboratorDetailResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerCollaboratorListItemResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.exception.ColaboratorNotFoundException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerCollaboratorsService {

    private final ColaboratorRepository colaboratorRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyService journeyService;
    private final AppTimeService appTimeService;

    @Transactional(readOnly = true)
    public Slice<ManagerCollaboratorListItemResponse> list(String search, int page, int size) {
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(page, size);
        Slice<Colaborator> collaborators = colaboratorRepository.findCollaboratorsForManager(
                Role.COLLABORATOR, normalizedSearch, pageable);

        if (collaborators.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        List<Long> collaboratorIds = collaborators.getContent().stream()
                .map(Colaborator::getId)
                .toList();
        Instant startedAtFrom = todayStart();
        Instant startedAtToExclusive = todayEndExclusive();
        List<Journey> journeysToday = journeyRepository
                .findByColaborator_IdInAndStartedAtBetweenWithPlannedActivities(
                        collaboratorIds, startedAtFrom, startedAtToExclusive);
        Map<Long, List<Journey>> journeysByCollaborator = journeysToday.stream()
                .collect(Collectors.groupingBy(j -> j.getColaborator().getId()));

        Instant now = Instant.now();
        List<ManagerCollaboratorListItemResponse> items = new ArrayList<>(collaborators.getNumberOfElements());
        for (Colaborator colaborator : collaborators.getContent()) {
            items.add(toListItem(colaborator, journeysByCollaborator.getOrDefault(colaborator.getId(), List.of()), now));
        }

        return new SliceImpl<>(items, pageable, collaborators.hasNext());
    }

    @Transactional(readOnly = true)
    public ManagerCollaboratorDetailResponse getById(Long id) {
        Colaborator colaborator = colaboratorRepository.findByIdAndUser_Role(id, Role.COLLABORATOR)
                .orElseThrow(() -> new ColaboratorNotFoundException(
                        "Colaborador com id " + id + " não encontrado."));

        Instant startedAtFrom = todayStart();
        Instant startedAtToExclusive = todayEndExclusive();
        long completedSeconds = journeyRepository.sumCompletedDurationSecondsByColaboratorAndStartedAtBetween(
                colaborator.getId(), startedAtFrom, startedAtToExclusive);

        Optional<Journey> inProgress = journeyRepository.findInProgressByColaborator_IdWithPlannedActivities(
                colaborator.getId());

        Instant now = Instant.now();
        long hoursTodaySeconds = completedSeconds;
        if (inProgress.isPresent()) {
            hoursTodaySeconds += Duration.between(inProgress.get().getStartedAt(), now).getSeconds();
            if (hoursTodaySeconds < 0) {
                hoursTodaySeconds = 0;
            }
        }

        Integer adherencePercentage = inProgress.map(this::computeAdherencePercentage).orElse(null);
        JourneyResponse currentJourney = inProgress.map(journeyService::mapToResponse).orElse(null);

        return new ManagerCollaboratorDetailResponse(
                colaborator.getId(),
                colaborator.getUser().getId(),
                colaborator.getFirstName(),
                hoursTodaySeconds,
                adherencePercentage,
                currentJourney);
    }

    private ManagerCollaboratorListItemResponse toListItem(
            Colaborator colaborator,
            List<Journey> journeysToday,
            Instant now) {
        Optional<Journey> inProgress = journeysToday.stream()
                .filter(j -> j.getStatus() == JourneyStatus.IN_PROGRESS)
                .findFirst();

        long completedSeconds = journeysToday.stream()
                .filter(j -> j.getStatus() == JourneyStatus.COMPLETED && j.getDurationSeconds() != null)
                .mapToLong(Journey::getDurationSeconds)
                .sum();

        long hoursTodaySeconds = completedSeconds;
        if (inProgress.isPresent()) {
            long inProgressSeconds = Duration.between(inProgress.get().getStartedAt(), now).getSeconds();
            hoursTodaySeconds += Math.max(inProgressSeconds, 0);
        }

        CollaboratorCurrentJourneyStatus currentJourneyStatus = resolveCurrentJourneyStatus(journeysToday, inProgress);
        Integer adherencePercentage = inProgress.map(this::computeAdherencePercentage).orElse(null);

        return new ManagerCollaboratorListItemResponse(
                colaborator.getId(),
                colaborator.getFirstName(),
                currentJourneyStatus,
                hoursTodaySeconds,
                adherencePercentage);
    }

    private CollaboratorCurrentJourneyStatus resolveCurrentJourneyStatus(
            List<Journey> journeysToday,
            Optional<Journey> inProgress) {
        if (inProgress.isPresent()) {
            return CollaboratorCurrentJourneyStatus.IN_PROGRESS;
        }
        boolean hasCompletedToday = journeysToday.stream()
                .anyMatch(j -> j.getStatus() == JourneyStatus.COMPLETED);
        if (hasCompletedToday) {
            return CollaboratorCurrentJourneyStatus.COMPLETED;
        }
        return CollaboratorCurrentJourneyStatus.NONE;
    }

    private Integer computeAdherencePercentage(Journey journey) {
        List<JourneyPlannedActivity> planned = journey.getPlannedActivities();
        if (planned.isEmpty()) {
            return null;
        }
        long checkedCount = planned.stream().filter(JourneyPlannedActivity::isChecked).count();
        return Math.round(100.0f * checkedCount / planned.size());
    }

    private Instant todayStart() {
        return LocalDate.now(appTimeService.zone()).atStartOfDay(appTimeService.zone()).toInstant();
    }

    private Instant todayEndExclusive() {
        return LocalDate.now(appTimeService.zone()).plusDays(1).atStartOfDay(appTimeService.zone()).toInstant();
    }

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

}
