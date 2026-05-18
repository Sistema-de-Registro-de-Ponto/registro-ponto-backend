package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, Long> {

    boolean existsByColaborator_IdAndEndedAtIsNullAndStatus(Long colaboratorId, JourneyStatus status);

    Optional<Journey> findByColaborator_IdAndEndedAtIsNullAndStatus(Long colaboratorId, JourneyStatus status);

    Optional<Journey> findByIdAndColaborator_Id(Long id, Long colaboratorId);

    long countByColaborator_IdAndStatus(Long colaboratorId, JourneyStatus status);

    Slice<Journey> findByColaborator_IdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            Long colaboratorId,
            Instant startedAtFrom,
            Instant startedAtToExclusive,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(j.durationSeconds), 0)
            FROM Journey j
            WHERE j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
              AND j.status = br.com.playercontabilidade.registroponto.entity.JourneyStatus.COMPLETED
            """)
    long sumDurationSecondsByStartedAtBetween(
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);

    @Query("""
            SELECT COUNT(j)
            FROM Journey j
            WHERE j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
              AND j.status = br.com.playercontabilidade.registroponto.entity.JourneyStatus.IN_PROGRESS
            """)
    long countInProgressByStartedAtBetween(
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);

    @Query("""
            SELECT DISTINCT j
            FROM Journey j
            LEFT JOIN FETCH j.plannedActivities
            WHERE j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
            """)
    List<Journey> findByStartedAtBetweenWithPlannedActivities(
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);

    @Query("""
            SELECT j FROM Journey j
            LEFT JOIN FETCH j.plannedActivities
            WHERE j.colaborator.id IN :colaboratorIds
              AND j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
            """)
    List<Journey> findByColaborator_IdInAndStartedAtBetweenWithPlannedActivities(
            @Param("colaboratorIds") Collection<Long> colaboratorIds,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);

    @Query("""
            SELECT j FROM Journey j
            LEFT JOIN FETCH j.plannedActivities
            WHERE j.colaborator.id = :colaboratorId
              AND j.status = br.com.playercontabilidade.registroponto.entity.JourneyStatus.IN_PROGRESS
              AND j.endedAt IS NULL
            """)
    Optional<Journey> findInProgressByColaborator_IdWithPlannedActivities(
            @Param("colaboratorId") Long colaboratorId);

    @Query("""
            SELECT COALESCE(SUM(j.durationSeconds), 0)
            FROM Journey j
            WHERE j.colaborator.id = :colaboratorId
              AND j.startedAt >= :startedAtFrom
              AND j.startedAt < :startedAtToExclusive
              AND j.status = br.com.playercontabilidade.registroponto.entity.JourneyStatus.COMPLETED
            """)
    long sumCompletedDurationSecondsByColaboratorAndStartedAtBetween(
            @Param("colaboratorId") Long colaboratorId,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive);

    @Query(
            value = """
                    SELECT j FROM Journey j
                    JOIN FETCH j.colaborator c
                    JOIN c.user u
                    WHERE u.role = :role
                      AND j.startedAt >= :startedAtFrom
                      AND j.startedAt < :startedAtToExclusive
                      AND (:collaboratorName IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :collaboratorName, '%')))
                    ORDER BY j.startedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(j) FROM Journey j
                    JOIN j.colaborator c
                    JOIN c.user u
                    WHERE u.role = :role
                      AND j.startedAt >= :startedAtFrom
                      AND j.startedAt < :startedAtToExclusive
                      AND (:collaboratorName IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :collaboratorName, '%')))
                    """)
    Page<Journey> findForManager(
            @Param("role") Role role,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive,
            @Param("collaboratorName") String collaboratorName,
            Pageable pageable);

    @Query("""
            SELECT j FROM Journey j
            JOIN FETCH j.colaborator c
            JOIN c.user u
            WHERE j.id = :id
              AND u.role = :role
            """)
    Optional<Journey> findByIdForManager(@Param("id") Long id, @Param("role") Role role);
}
