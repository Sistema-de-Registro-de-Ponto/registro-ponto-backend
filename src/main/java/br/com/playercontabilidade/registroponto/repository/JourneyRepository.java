package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
}
