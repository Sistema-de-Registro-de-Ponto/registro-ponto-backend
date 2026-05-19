package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.RpaRecordResponse;
import br.com.playercontabilidade.registroponto.entity.RpaRecord;
import br.com.playercontabilidade.registroponto.exception.InvalidDateRangeException;
import br.com.playercontabilidade.registroponto.repository.RpaRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ManagerRpaRecordsService {

    private final RpaRecordRepository rpaRecordRepository;
    private final AppTimeService appTimeService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<RpaRecordResponse> list(
            LocalDate startDate,
            LocalDate endDate,
            String search,
            int page,
            int size) {
        DateRange range = resolveDateRange(startDate, endDate);
        String normalizedSearch = normalizeSearch(search);

        Pageable pageable = PageRequest.of(page, size);
        return rpaRecordRepository
                .findForManager(range.startDate(), range.endDate(), normalizedSearch, pageable)
                .map(this::toResponse);
    }

    private RpaRecordResponse toResponse(RpaRecord record) {
        Long collaboratorId = record.getColaborator() != null ? record.getColaborator().getId() : null;
        String collaboratorFirstName = record.getColaborator() != null
                ? record.getColaborator().getFirstName()
                : null;

        return new RpaRecordResponse(
                record.getId(),
                record.getSourceSystem(),
                record.getExternalEmployeeId(),
                record.getEmployeeName(),
                record.getWorkDate(),
                appTimeService.toOffsetDateTime(record.getCheckInAt()),
                record.getCheckOutAt() != null ? appTimeService.toOffsetDateTime(record.getCheckOutAt()) : null,
                record.getWorkedSeconds(),
                deserializeRawPayload(record.getRawPayload()),
                appTimeService.toOffsetDateTime(record.getImportedAt()),
                collaboratorId,
                collaboratorFirstName);
    }

    private JsonNode deserializeRawPayload(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return null;
        }
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(appTimeService.zone());
        LocalDate resolvedStart = startDate != null ? startDate : today;
        LocalDate resolvedEnd = endDate != null ? endDate : today;

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new InvalidDateRangeException("start_date não pode ser posterior a end_date.");
        }

        return new DateRange(resolvedStart, resolvedEnd);
    }

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
