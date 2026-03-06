package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, Long> {

    Page<WalletTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
