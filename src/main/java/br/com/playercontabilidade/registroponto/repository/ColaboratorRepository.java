package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ColaboratorRepository extends JpaRepository<Colaborator, Long> {

    Optional<Colaborator> findByUser_Username(String username);

    boolean existsByUser_Id(Long userId);

    Optional<Colaborator> findByIdAndUser_Role(Long id, Role role);

    @Query(
            value = """
                    SELECT c FROM Colaborator c
                    JOIN c.user u
                    WHERE u.role = :role
                      AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
                    ORDER BY c.firstName ASC, c.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(c) FROM Colaborator c
                    JOIN c.user u
                    WHERE u.role = :role
                      AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
                    """)
    Page<Colaborator> findCollaboratorsForManager(
            @Param("role") Role role,
            @Param("search") String search,
            Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT c FROM Colaborator c
                    JOIN c.user u
                    WHERE u.role = :role
                      AND EXISTS (
                        SELECT 1 FROM Journey j
                        WHERE j.colaborator = c
                          AND j.startedAt >= :startedAtFrom
                          AND j.startedAt < :startedAtToExclusive
                      )
                      AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
                    ORDER BY c.firstName ASC, c.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT c) FROM Colaborator c
                    JOIN c.user u
                    WHERE u.role = :role
                      AND EXISTS (
                        SELECT 1 FROM Journey j
                        WHERE j.colaborator = c
                          AND j.startedAt >= :startedAtFrom
                          AND j.startedAt < :startedAtToExclusive
                      )
                      AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
                    """)
    Page<Colaborator> findWithJourneysInPeriodForManager(
            @Param("role") Role role,
            @Param("startedAtFrom") Instant startedAtFrom,
            @Param("startedAtToExclusive") Instant startedAtToExclusive,
            @Param("search") String search,
            Pageable pageable);
}
