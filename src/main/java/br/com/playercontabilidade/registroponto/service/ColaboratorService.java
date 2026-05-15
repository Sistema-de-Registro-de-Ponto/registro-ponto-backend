package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.ColaboratorProfileResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.exception.ColaboratorNotFoundException;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ColaboratorService {

    private final ColaboratorRepository colaboratorRepository;

    @Transactional(readOnly = true)
    public ColaboratorProfileResponse getProfileForCurrentUser(String username) {
        Colaborator colaborator = colaboratorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ColaboratorNotFoundException(
                        "Não existe colaborador associado ao usuário autenticado."));
        return new ColaboratorProfileResponse(
                colaborator.getUser().getId(),
                colaborator.getFirstName());
    }
}
