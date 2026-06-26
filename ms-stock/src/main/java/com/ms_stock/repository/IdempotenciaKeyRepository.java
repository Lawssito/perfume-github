package com.ms_stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ms_stock.model.IdempotenciaKey;

import java.util.Optional;

@Repository
public interface IdempotenciaKeyRepository extends JpaRepository<IdempotenciaKey, Long> {

    Optional<IdempotenciaKey> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
