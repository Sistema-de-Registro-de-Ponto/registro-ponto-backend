package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.UnplannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
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

    @Query("""
            SELECT COUNT(ua)
            FROM UnplannedActivity ua
            JOIN ua.journey j
            JOIN j.colaborator c
            JOIN c.user u
            WHERE u.role = :role
              AND j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
              AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    long countByJourneyStartedAtBetweenForManager(
            @Param("role") Role role,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive,
            @Param("search") String search);

    @Query("""
            SELECT j.colaborator.id, COUNT(ua)
            FROM UnplannedActivity ua
            JOIN ua.journey j
            WHERE j.colaborator.id IN :collaboratorIds
              AND j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
            GROUP BY j.colaborator.id
            """)
    List<Object[]> countGroupedByCollaboratorIdAndJourneyStartedAtBetween(
            @Param("collaboratorIds") Collection<Long> collaboratorIds,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);
}
