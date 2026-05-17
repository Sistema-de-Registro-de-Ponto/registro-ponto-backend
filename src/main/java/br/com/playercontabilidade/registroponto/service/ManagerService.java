package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.dto.ManagerProfileResponse;
import br.com.playercontabilidade.registroponto.entity.Manager;
import br.com.playercontabilidade.registroponto.exception.ManagerNotFoundException;
import br.com.playercontabilidade.registroponto.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final ManagerRepository managerRepository;

    @Transactional(readOnly = true)
    public ManagerProfileResponse getProfileForCurrentUser(String username) {
        Manager manager = managerRepository.findByUser_Username(username)
                .orElseThrow(() -> new ManagerNotFoundException(
                        "Não existe gerente associado ao usuário autenticado."));
        return new ManagerProfileResponse(
                manager.getUser().getId(),
                manager.getFirstName());
    }
}
