package com.group09.ComicReader.wallet.service;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.wallet.dto.TopUpRequest;
import com.group09.ComicReader.wallet.dto.TransactionResponse;
import com.group09.ComicReader.wallet.dto.WalletResponse;
import com.group09.ComicReader.wallet.entity.TransactionType;
import com.group09.ComicReader.wallet.entity.UserWalletEntity;
import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import com.group09.ComicReader.wallet.repository.UserWalletRepository;
import com.group09.ComicReader.wallet.repository.WalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletService(UserWalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ── Read ────────────────────────────────────────────

    public WalletResponse getWallet() {
        UserEntity user = getCurrentUser();
        UserWalletEntity wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> createWallet(user));
        return toWalletResponse(wallet);
    }

    public Page<TransactionResponse> getTransactions(Pageable pageable) {
        UserEntity user = getCurrentUser();
        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toTransactionResponse);
    }

    // ── Write ───────────────────────────────────────────

    @Transactional
    public WalletResponse topUp(TopUpRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        UserEntity user = getCurrentUser();

        // Lock the wallet row for update
        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        String currency = request.getCurrency() != null ? request.getCurrency() : "COIN";
        int newBalance;

        if ("POINT".equalsIgnoreCase(currency)) {
            wallet.setPointBalance(wallet.getPointBalance() + request.getAmount());
            newBalance = wallet.getPointBalance();
        } else {
            wallet.setCoinBalance(wallet.getCoinBalance() + request.getAmount());
            newBalance = wallet.getCoinBalance();
        }
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Record transaction
        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setUser(user);
        tx.setType(TransactionType.TOP_UP);
        tx.setAmount(request.getAmount());
        tx.setCurrency(currency.toUpperCase());
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Top-up " + request.getAmount() + " " + currency);
        tx.setReferenceId(request.getReferenceId());
        transactionRepository.save(tx);

        return toWalletResponse(wallet);
    }

    // ── Helpers ─────────────────────────────────────────

    private UserWalletEntity createWallet(UserEntity user) {
        UserWalletEntity wallet = new UserWalletEntity();
        wallet.setUser(user);
        wallet.setCoinBalance(0);
        wallet.setPointBalance(0);
        return walletRepository.save(wallet);
    }

    public UserEntity getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private WalletResponse toWalletResponse(UserWalletEntity entity) {
        WalletResponse r = new WalletResponse();
        r.setId(entity.getId());
        r.setUserId(entity.getUser().getId());
        r.setCoinBalance(entity.getCoinBalance());
        r.setPointBalance(entity.getPointBalance());
        return r;
    }

    private TransactionResponse toTransactionResponse(WalletTransactionEntity entity) {
        TransactionResponse r = new TransactionResponse();
        r.setId(entity.getId());
        r.setType(entity.getType().name());
        r.setAmount(entity.getAmount());
        r.setCurrency(entity.getCurrency());
        r.setBalanceAfter(entity.getBalanceAfter());
        r.setDescription(entity.getDescription());
        r.setReferenceId(entity.getReferenceId());
        r.setCreatedAt(entity.getCreatedAt());
        return r;
    }
}
