package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.ManagerOverviewResponse;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.exception.InvalidDateRangeException;
import br.com.playercontabilidade.registroponto.repository.JourneyPlannedActivityRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import br.com.playercontabilidade.registroponto.repository.UnplannedActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerOverviewService {

    private final JourneyRepository journeyRepository;
    private final JourneyPlannedActivityRepository journeyPlannedActivityRepository;
    private final UnplannedActivityRepository unplannedActivityRepository;
    private final AppTimeService appTimeService;

    @Transactional(readOnly = true)
    public ManagerOverviewResponse getOverview(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(appTimeService.zone());
        LocalDate resolvedStart = startDate != null ? startDate : today;
        LocalDate resolvedEnd = endDate != null ? endDate : today;

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new InvalidDateRangeException(
                    "start_date não pode ser posterior a end_date.");
        }

        Instant startedAtFrom = resolvedStart.atStartOfDay(appTimeService.zone()).toInstant();
        Instant startedAtToExclusive = resolvedEnd.plusDays(1).atStartOfDay(appTimeService.zone()).toInstant();

        long durationSeconds = journeyRepository.sumDurationSecondsByStartedAtBetween(
                startedAtFrom, startedAtToExclusive);
        long journeysProgress = journeyRepository.countInProgressByStartedAtBetween(
                startedAtFrom, startedAtToExclusive);
        long activitiesCompleted = journeyPlannedActivityRepository.countCheckedByJourneyStartedAtBetween(
                startedAtFrom, startedAtToExclusive);
        long unplannedActivities = unplannedActivityRepository.countByJourneyStartedAtBetween(
                startedAtFrom, startedAtToExclusive);
        int averageAdherencePercentage = computeAverageAdherencePercentage(
                startedAtFrom, startedAtToExclusive);

        return new ManagerOverviewResponse(
                durationSeconds,
                journeysProgress,
                averageAdherencePercentage,
                activitiesCompleted,
                unplannedActivities);
    }

    private int computeAverageAdherencePercentage(Instant startedAtFrom, Instant startedAtToExclusive) {
        List<Journey> journeys = journeyRepository.findByStartedAtBetweenWithPlannedActivities(
                startedAtFrom, startedAtToExclusive);

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
}
