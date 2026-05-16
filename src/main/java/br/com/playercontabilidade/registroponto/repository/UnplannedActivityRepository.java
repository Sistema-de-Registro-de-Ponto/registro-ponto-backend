package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.UnplannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnplannedActivityRepository extends JpaRepository<UnplannedActivity, Long> {

    Optional<UnplannedActivity> findByIdAndJourney_Colaborator_Id(Long id, Long collaboratorId);
}
