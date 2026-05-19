package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.RpaRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RpaRecordRepository extends JpaRepository<RpaRecord, Long> {

    @Query(
            value = """
                    SELECT r FROM RpaRecord r
                    WHERE r.workDate >= :startDate
                      AND r.workDate <= :endDate
                      AND (:search IS NULL OR LOWER(r.employeeName) LIKE LOWER(CONCAT('%', :search, '%')))
                    ORDER BY r.workDate DESC, r.checkInAt DESC, r.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM RpaRecord r
                    WHERE r.workDate >= :startDate
                      AND r.workDate <= :endDate
                      AND (:search IS NULL OR LOWER(r.employeeName) LIKE LOWER(CONCAT('%', :search, '%')))
                    """)
    Page<RpaRecord> findForManager(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable);
}
