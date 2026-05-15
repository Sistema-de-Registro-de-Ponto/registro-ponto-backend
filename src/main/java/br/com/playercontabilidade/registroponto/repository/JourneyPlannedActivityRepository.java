package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JourneyPlannedActivityRepository extends JpaRepository<JourneyPlannedActivity, Long> {

    Optional<JourneyPlannedActivity> findByIdAndJourney_Colaborator_IdAndJourney_EndedAtIsNullAndJourney_Status(
            Long id,
            Long collaboratorId,
            JourneyStatus status);
}
