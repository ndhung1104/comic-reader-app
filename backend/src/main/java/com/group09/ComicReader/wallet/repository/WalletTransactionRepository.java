package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.TransactionType;
import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, Long> {

    Page<WalletTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<WalletTransactionEntity> findFirstByUserIdAndTypeAndReferenceId(Long userId,
                                                                              TransactionType type,
                                                                              String referenceId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
           "WHERE t.type = :type AND t.createdAt >= :from AND t.createdAt < :to")
    Long sumAmountByTypeAndDateRange(@Param("type") TransactionType type,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(t) FROM WalletTransactionEntity t " +
           "WHERE t.createdAt >= :from AND t.createdAt < :to")
    Long countByDateRange(@Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to);

    @Query("SELECT t FROM WalletTransactionEntity t " +
           "WHERE t.createdAt >= :from AND t.createdAt < :to " +
           "ORDER BY t.createdAt ASC")
    List<WalletTransactionEntity> findAllByDateRange(@Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);
}
