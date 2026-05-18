package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Role;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColaboratorRepository extends JpaRepository<Colaborator, Long> {

    Optional<Colaborator> findByUser_Username(String username);

    boolean existsByUser_Id(Long userId);

    Optional<Colaborator> findByIdAndUser_Role(Long id, Role role);

    @Query("""
            SELECT c FROM Colaborator c
            JOIN c.user u
            WHERE u.role = :role
              AND (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY c.firstName ASC, c.id ASC
            """)
    Slice<Colaborator> findCollaboratorsForManager(
            @Param("role") Role role,
            @Param("search") String search,
            Pageable pageable);
}
