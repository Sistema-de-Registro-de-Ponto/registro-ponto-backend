package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.PlannedActivityRequest;
import br.com.playercontabilidade.registroponto.dto.PlannedActivityResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.PlannedActivity;
import br.com.playercontabilidade.registroponto.exception.ColaboratorNotFoundException;
import br.com.playercontabilidade.registroponto.exception.PlannedActivityNotFoundException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.PlannedActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlannedActivityService {

    private final ColaboratorRepository colaboratorRepository;
    private final PlannedActivityRepository plannedActivityRepository;
    private final AppTimeService appTimeService;

    @Transactional
    public PlannedActivityResponse create(String username, PlannedActivityRequest request) {
        Colaborator colaborator = resolveColaborator(username);
        PlannedActivity activity = plannedActivityRepository.save(PlannedActivity.builder()
                .colaborator(colaborator)
                .description(request.description().trim())
                .build());
        return toResponse(activity);
    }

    @Transactional(readOnly = true)
    public List<PlannedActivityResponse> listForCurrentUser(String username) {
        Colaborator colaborator = resolveColaborator(username);
        return plannedActivityRepository.findByColaborator_IdOrderByCreatedAtAsc(colaborator.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlannedActivityResponse delete(String username, Long id) {
        Colaborator colaborator = resolveColaborator(username);
        PlannedActivity activity = plannedActivityRepository.findByIdAndColaborator_Id(id, colaborator.getId())
                .orElseThrow(() -> new PlannedActivityNotFoundException(
                        "Atividade planejada não encontrada."));
        plannedActivityRepository.delete(activity);
        return toResponse(activity);
    }

    private Colaborator resolveColaborator(String username) {
        return colaboratorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ColaboratorNotFoundException(
                        "Não existe colaborador associado ao usuário autenticado."));
    }

    private PlannedActivityResponse toResponse(PlannedActivity activity) {
        return new PlannedActivityResponse(
                activity.getId(),
                activity.getDescription(),
                appTimeService.toOffsetDateTime(activity.getCreatedAt()));
    }
}
