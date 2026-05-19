package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.RpaImportRecordRequest;
import br.com.playercontabilidade.registroponto.dto.RpaImportRequest;
import br.com.playercontabilidade.registroponto.dto.RpaImportResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.RpaRecord;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.exception.InvalidRpaImportException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.RpaRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RpaImportService {

    private final RpaRecordRepository rpaRecordRepository;
    private final ColaboratorRepository colaboratorRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RpaImportResponse importRecords(RpaImportRequest request) {
        Instant importedAt = Instant.now();
        List<Long> ids = new ArrayList<>();

        for (RpaImportRecordRequest item : request.records()) {
            Instant checkInAt = item.checkInAt().toInstant();
            Instant checkOutAt = item.checkOutAt() != null ? item.checkOutAt().toInstant() : null;
            validateCheckOut(checkInAt, checkOutAt);

            Long workedSeconds = resolveWorkedSeconds(item.workedSeconds(), checkInAt, checkOutAt);
            Colaborator colaborator = resolveCollaborator(item.employeeName());

            RpaRecord record = RpaRecord.builder()
                    .sourceSystem(request.sourceSystem().trim())
                    .externalEmployeeId(normalizeOptional(item.externalEmployeeId()))
                    .employeeName(item.employeeName().trim())
                    .workDate(item.workDate())
                    .checkInAt(checkInAt)
                    .checkOutAt(checkOutAt)
                    .workedSeconds(workedSeconds)
                    .rawPayload(serializeRawPayload(item.rawPayload()))
                    .importedAt(importedAt)
                    .colaborator(colaborator)
                    .build();

            RpaRecord saved = rpaRecordRepository.save(record);
            ids.add(saved.getId());
        }

        return new RpaImportResponse(ids.size(), ids);
    }

    private void validateCheckOut(Instant checkInAt, Instant checkOutAt) {
        if (checkOutAt != null && checkOutAt.isBefore(checkInAt)) {
            throw new InvalidRpaImportException("check_out_at não pode ser anterior a check_in_at.");
        }
    }

    private Long resolveWorkedSeconds(Long provided, Instant checkInAt, Instant checkOutAt) {
        if (provided != null) {
            return provided;
        }
        if (checkOutAt == null) {
            return null;
        }
        return Duration.between(checkInAt, checkOutAt).getSeconds();
    }

    private Colaborator resolveCollaborator(String employeeName) {
        return colaboratorRepository
                .findFirstByFirstNameIgnoreCaseAndUser_Role(employeeName.trim(), Role.COLLABORATOR)
                .orElse(null);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String serializeRawPayload(com.fasterxml.jackson.databind.JsonNode rawPayload) {
        if (rawPayload == null || rawPayload.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rawPayload);
        } catch (JsonProcessingException e) {
            throw new InvalidRpaImportException("raw_payload inválido.");
        }
    }
}
