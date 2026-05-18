package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.JourneyPlannedActivity;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface JourneyPlannedActivityRepository extends JpaRepository<JourneyPlannedActivity, Long> {

    Optional<JourneyPlannedActivity> findByIdAndJourney_Colaborator_IdAndJourney_EndedAtIsNullAndJourney_Status(
            Long id,
            Long collaboratorId,
            JourneyStatus status);

    @Query("""
            SELECT COUNT(jpa)
            FROM JourneyPlannedActivity jpa
            WHERE jpa.checked = true
              AND jpa.journey.startedAt >= :startedAtFrom
              AND jpa.journey.startedAt < :startedAtToExclusive
            """)
    long countCheckedByJourneyStartedAtBetween(
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);

    @Query("""
            SELECT COUNT(jpa)
            FROM JourneyPlannedActivity jpa
            JOIN jpa.journey j
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
            SELECT COUNT(jpa)
            FROM JourneyPlannedActivity jpa
            JOIN jpa.journey j
            JOIN j.colaborator c
            JOIN c.user u
            WHERE u.role = :role
              AND jpa.checked = true
              AND j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
              AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    long countCheckedByJourneyStartedAtBetweenForManager(
            @Param("role") Role role,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive,
            @Param("search") String search);
}
