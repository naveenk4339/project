package com.pos.checkout.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosTransactionRepository extends JpaRepository<PosTransaction, String> {

    Optional<PosTransaction> findByIdempotencyKey(String idempotencyKey);

    List<PosTransaction> findByStoreIdOrderByCreatedAtDesc(String storeId, Pageable pageable);
}
