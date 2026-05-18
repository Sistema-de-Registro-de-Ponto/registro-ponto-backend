package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.UnplannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UnplannedActivityRepository extends JpaRepository<UnplannedActivity, Long> {

    Optional<UnplannedActivity> findByIdAndJourney_Colaborator_Id(Long id, Long collaboratorId);

    @Query("""
            SELECT COUNT(ua)
            FROM UnplannedActivity ua
            WHERE ua.journey.startedAt >= :startedAtFrom
              AND ua.journey.startedAt < :startedAtToExclusive
            """)
    long countByJourneyStartedAtBetween(
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);
}
