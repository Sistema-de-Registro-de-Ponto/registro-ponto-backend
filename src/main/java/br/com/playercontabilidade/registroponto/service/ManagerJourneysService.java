package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.JourneyResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerJourneyDetailResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerJourneyListItemResponse;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.exception.InvalidDateRangeException;
import br.com.playercontabilidade.registroponto.exception.JourneyNotFoundException;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ManagerJourneysService {

    private final JourneyRepository journeyRepository;
    private final JourneyService journeyService;
    private final AppTimeService appTimeService;

    @Transactional(readOnly = true)
    public Page<ManagerJourneyListItemResponse> list(
            LocalDate startDate,
            LocalDate endDate,
            String collaboratorName,
            int page,
            int size) {
        DateRange range = resolveDateRange(startDate, endDate);
        String normalizedName = normalizeCollaboratorName(collaboratorName);

        Pageable pageable = PageRequest.of(page, size);
        Page<Journey> journeys = journeyRepository.findForManager(
                Role.COLLABORATOR,
                range.startedAtFrom(),
                range.startedAtToExclusive(),
                normalizedName,
                pageable);

        Instant now = Instant.now();
        return journeys.map(journey -> toListItem(journey, now));
    }

    @Transactional(readOnly = true)
    public ManagerJourneyDetailResponse getById(Long id) {
        Journey journey = journeyRepository.findByIdForManager(id, Role.COLLABORATOR)
                .orElseThrow(() -> new JourneyNotFoundException(
                        "Jornada com id " + id + " não encontrada."));

        JourneyResponse journeyResponse = journeyService.mapToResponse(journey);
        return new ManagerJourneyDetailResponse(
                journeyResponse.id(),
                journeyResponse.collaboratorId(),
                journey.getColaborator().getFirstName(),
                journeyResponse.startedAt(),
                journeyResponse.endedAt(),
                journeyResponse.durationSeconds(),
                journeyResponse.summary(),
                journeyResponse.plannedActivities(),
                journeyResponse.unplannedActivities(),
                journeyResponse.status(),
                journeyResponse.createdAt(),
                journeyResponse.updatedAt());
    }

    private ManagerJourneyListItemResponse toListItem(Journey journey, Instant now) {
        long durationSeconds = resolveDurationSeconds(journey, now);
        LocalDate journeyDate = journey.getStartedAt()
                .atZone(appTimeService.zone())
                .toLocalDate();

        return new ManagerJourneyListItemResponse(
                journey.getId(),
                journeyDate,
                journey.getColaborator().getId(),
                journey.getColaborator().getFirstName(),
                appTimeService.toOffsetDateTime(journey.getStartedAt()),
                journey.getEndedAt() != null
                        ? appTimeService.toOffsetDateTime(journey.getEndedAt())
                        : null,
                durationSeconds,
                journey.getStatus());
    }

    private long resolveDurationSeconds(Journey journey, Instant now) {
        if (journey.getStatus() == JourneyStatus.COMPLETED && journey.getDurationSeconds() != null) {
            return journey.getDurationSeconds();
        }
        long elapsed = Duration.between(journey.getStartedAt(), now).getSeconds();
        return Math.max(elapsed, 0);
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(appTimeService.zone());
        LocalDate resolvedStart = startDate != null ? startDate : today;
        LocalDate resolvedEnd = endDate != null ? endDate : today;

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new InvalidDateRangeException(
                    "start_date não pode ser posterior a end_date.");
        }

        Instant startedAtFrom = resolvedStart.atStartOfDay(appTimeService.zone()).toInstant();
        Instant startedAtToExclusive = resolvedEnd.plusDays(1).atStartOfDay(appTimeService.zone()).toInstant();
        return new DateRange(startedAtFrom, startedAtToExclusive);
    }

    private String normalizeCollaboratorName(String collaboratorName) {
        if (!StringUtils.hasText(collaboratorName)) {
            return null;
        }
        return collaboratorName.trim();
    }

    private record DateRange(Instant startedAtFrom, Instant startedAtToExclusive) {
    }
}
