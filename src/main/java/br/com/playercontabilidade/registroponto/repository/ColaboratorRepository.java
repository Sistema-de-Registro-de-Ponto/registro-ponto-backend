package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColaboratorRepository extends JpaRepository<Colaborator, Long> {

    Optional<Colaborator> findByUser_Username(String username);

    boolean existsByUser_Id(Long userId);
}
