package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.ManagerConsolidatedReportCollaboratorItem;
import br.com.playercontabilidade.registroponto.dto.ManagerConsolidatedReportPeriod;
import br.com.playercontabilidade.registroponto.dto.ManagerConsolidatedReportResponse;
import br.com.playercontabilidade.registroponto.dto.ManagerConsolidatedReportSummary;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.exception.InvalidDateRangeException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyPlannedActivityRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import br.com.playercontabilidade.registroponto.repository.UnplannedActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerConsolidatedReportService {

    private final ColaboratorRepository colaboratorRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyPlannedActivityRepository journeyPlannedActivityRepository;
    private final UnplannedActivityRepository unplannedActivityRepository;
    private final AppTimeService appTimeService;

    @Transactional(readOnly = true)
    public ManagerConsolidatedReportResponse getReport(
            LocalDate startDate,
            LocalDate endDate,
            String search,
            int page,
            int size) {
        DateRange range = resolveDateRange(startDate, endDate);
        String normalizedSearch = normalizeSearch(search);

        ManagerConsolidatedReportSummary summary = buildSummary(range, normalizedSearch);

        Pageable pageable = PageRequest.of(page, size);
        Page<Colaborator> collaborators = colaboratorRepository.findWithJourneysInPeriodForManager(
                Role.COLLABORATOR,
                range.startedAtFrom(),
                range.startedAtToExclusive(),
                normalizedSearch,
                pageable);

        if (collaborators.isEmpty()) {
            return new ManagerConsolidatedReportResponse(
                    toPeriod(range),
                    summary,
                    Page.empty(pageable));
        }

        List<Long> collaboratorIds = collaborators.getContent().stream()
                .map(Colaborator::getId)
                .toList();

        List<Journey> journeys = journeyRepository.findByColaborator_IdInAndStartedAtBetweenWithPlannedActivities(
                collaboratorIds, range.startedAtFrom(), range.startedAtToExclusive());
        Map<Long, List<Journey>> journeysByCollaborator = journeys.stream()
                .collect(Collectors.groupingBy(j -> j.getColaborator().getId()));

        Map<Long, Long> unplannedByCollaborator = toCountMap(
                unplannedActivityRepository.countGroupedByCollaboratorIdAndJourneyStartedAtBetween(
                        collaboratorIds, range.startedAtFrom(), range.startedAtToExclusive()));

        List<ManagerConsolidatedReportCollaboratorItem> items = new ArrayList<>(collaborators.getNumberOfElements());
        for (Colaborator colaborator : collaborators.getContent()) {
            List<Journey> collaboratorJourneys = journeysByCollaborator.getOrDefault(
                    colaborator.getId(), List.of());
            long unplannedCount = unplannedByCollaborator.getOrDefault(colaborator.getId(), 0L);
            items.add(toCollaboratorItem(colaborator, collaboratorJourneys, unplannedCount));
        }

        Page<ManagerConsolidatedReportCollaboratorItem> collaboratorPage = new PageImpl<>(
                items, collaborators.getPageable(), collaborators.getTotalElements());

        return new ManagerConsolidatedReportResponse(toPeriod(range), summary, collaboratorPage);
    }

    private ManagerConsolidatedReportSummary buildSummary(DateRange range, String search) {
        long durationSeconds = journeyRepository.sumCompletedDurationSecondsForManagerInPeriod(
                Role.COLLABORATOR,
                range.startedAtFrom(),
                range.startedAtToExclusive(),
                search);
        long plannedActivities = journeyPlannedActivityRepository.countByJourneyStartedAtBetweenForManager(
                Role.COLLABORATOR,
                range.startedAtFrom(),
                range.startedAtToExclusive(),
                search);
        long activitiesCompleted = journeyPlannedActivityRepository.countCheckedByJourneyStartedAtBetweenForManager(
                Role.COLLABORATOR,
                range.startedAtFrom(),
                range.startedAtToExclusive(),
                search);
        long unplannedActivities = unplannedActivityRepository.countByJourneyStartedAtBetweenForManager(
                Role.COLLABORATOR,
                range.startedAtFrom(),
                range.startedAtToExclusive(),
                search);
        int averageAdherencePercentage = computeAverageAdherencePercentage(
                journeyRepository.findByStartedAtBetweenWithPlannedActivitiesForManager(
                        Role.COLLABORATOR,
                        range.startedAtFrom(),
                        range.startedAtToExclusive(),
                        search));

        return new ManagerConsolidatedReportSummary(
                durationSeconds,
                plannedActivities,
                activitiesCompleted,
                unplannedActivities,
                averageAdherencePercentage);
    }

    private ManagerConsolidatedReportCollaboratorItem toCollaboratorItem(
            Colaborator colaborator,
            List<Journey> journeys,
            long unplannedCount) {
        long durationSeconds = journeys.stream()
                .filter(j -> j.getStatus() == JourneyStatus.COMPLETED && j.getDurationSeconds() != null)
                .mapToLong(Journey::getDurationSeconds)
                .sum();

        long plannedActivities = journeys.stream()
                .mapToLong(j -> j.getPlannedActivities().size())
                .sum();

        long activitiesCompleted = journeys.stream()
                .flatMap(j -> j.getPlannedActivities().stream())
                .filter(JourneyPlannedActivity::isChecked)
                .count();

        int adherencePercentage = computeAverageAdherencePercentage(journeys);

        return new ManagerConsolidatedReportCollaboratorItem(
                colaborator.getId(),
                colaborator.getFirstName(),
                durationSeconds,
                plannedActivities,
                activitiesCompleted,
                unplannedCount,
                adherencePercentage);
    }

    private int computeAverageAdherencePercentage(List<Journey> journeys) {
        int adherenceSum = 0;
        int journeysWithPlanned = 0;

        for (Journey journey : journeys) {
            List<JourneyPlannedActivity> planned = journey.getPlannedActivities();
            if (planned.isEmpty()) {
                continue;
            }
            long checkedCount = planned.stream().filter(JourneyPlannedActivity::isChecked).count();
            adherenceSum += Math.round(100.0 * checkedCount / planned.size());
            journeysWithPlanned++;
        }

        if (journeysWithPlanned == 0) {
            return 0;
        }
        return adherenceSum / journeysWithPlanned;
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private ManagerConsolidatedReportPeriod toPeriod(DateRange range) {
        return new ManagerConsolidatedReportPeriod(range.resolvedStart(), range.resolvedEnd());
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
        return new DateRange(resolvedStart, resolvedEnd, startedAtFrom, startedAtToExclusive);
    }

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

    private record DateRange(
            LocalDate resolvedStart,
            LocalDate resolvedEnd,
            Instant startedAtFrom,
            Instant startedAtToExclusive) {
    }
}
