package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.PlannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlannedActivityRepository extends JpaRepository<PlannedActivity, Long> {

    List<PlannedActivity> findByColaborator_IdOrderByCreatedAtAsc(Long colaboratorId);

    Optional<PlannedActivity> findByIdAndColaborator_Id(Long id, Long colaboratorId);
}
