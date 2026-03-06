package com.group09.ComicReader.wallet.service;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.wallet.dto.*;
import com.group09.ComicReader.wallet.entity.ChapterPurchaseEntity;
import com.group09.ComicReader.wallet.entity.TransactionType;
import com.group09.ComicReader.wallet.entity.UserWalletEntity;
import com.group09.ComicReader.wallet.entity.VipSubscriptionEntity;
import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import com.group09.ComicReader.wallet.repository.ChapterPurchaseRepository;
import com.group09.ComicReader.wallet.repository.UserWalletRepository;
import com.group09.ComicReader.wallet.repository.VipSubscriptionRepository;
import com.group09.ComicReader.wallet.repository.WalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {

    private static final Map<String, Integer> VIP_PRICES = Map.of(
            "MONTHLY", 500,
            "YEARLY", 5000
    );

    private static final int AD_REWARD_COINS = 10;

    private static final Map<String, Integer> IAP_PRODUCTS = Map.of(
            "coins_100",  100,
            "coins_500",  500,
            "coins_1000", 1000,
            "coins_5000", 5000
    );

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final ChapterPurchaseRepository purchaseRepository;
    private final VipSubscriptionRepository vipRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;

    public WalletService(UserWalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         ChapterPurchaseRepository purchaseRepository,
                         VipSubscriptionRepository vipRepository,
                         ChapterRepository chapterRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.purchaseRepository = purchaseRepository;
        this.vipRepository = vipRepository;
        this.chapterRepository = chapterRepository;
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

    @Transactional
    public WalletResponse purchaseChapter(PurchaseChapterRequest request) {
        UserEntity user = getCurrentUser();

        ChapterEntity chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new NotFoundException("Chapter not found: " + request.getChapterId()));

        if (!chapter.isPremium()) {
            throw new BadRequestException("Chapter is not premium");
        }

        int price = chapter.getPrice() != null ? chapter.getPrice() : 0;
        if (price <= 0) {
            throw new BadRequestException("Chapter has no price set");
        }

        // Check already purchased
        if (purchaseRepository.existsByUserIdAndChapterId(user.getId(), chapter.getId())) {
            throw new BadRequestException("Chapter already purchased");
        }

        // Lock the wallet row
        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        String currency = request.getCurrency() != null ? request.getCurrency() : "COIN";
        int newBalance;

        if ("POINT".equalsIgnoreCase(currency)) {
            if (wallet.getPointBalance() < price) {
                throw new BadRequestException("Insufficient point balance");
            }
            wallet.setPointBalance(wallet.getPointBalance() - price);
            newBalance = wallet.getPointBalance();
        } else {
            if (wallet.getCoinBalance() < price) {
                throw new BadRequestException("Insufficient coin balance");
            }
            wallet.setCoinBalance(wallet.getCoinBalance() - price);
            newBalance = wallet.getCoinBalance();
        }
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Record purchase
        ChapterPurchaseEntity purchase = new ChapterPurchaseEntity();
        purchase.setUser(user);
        purchase.setChapter(chapter);
        purchase.setPricePaid(price);
        purchase.setCurrency(currency.toUpperCase());
        purchaseRepository.save(purchase);

        // Record transaction
        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setUser(user);
        tx.setType(TransactionType.PURCHASE);
        tx.setAmount(price);
        tx.setCurrency(currency.toUpperCase());
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Purchase chapter: " + chapter.getTitle());
        tx.setReferenceId("chapter-" + chapter.getId());
        transactionRepository.save(tx);

        return toWalletResponse(wallet);
    }

    // ── VIP ──────────────────────────────────────────────

    public VipStatusResponse getVipStatus() {
        UserEntity user = getCurrentUser();
        Optional<VipSubscriptionEntity> active =
                vipRepository.findActiveByUserId(user.getId(), LocalDateTime.now());

        VipStatusResponse r = new VipStatusResponse();
        if (active.isPresent()) {
            VipSubscriptionEntity v = active.get();
            r.setVip(true);
            r.setPlan(v.getPlan());
            r.setStartDate(v.getStartDate().toString());
            r.setEndDate(v.getEndDate().toString());
            r.setStatus(v.getStatus());
        } else {
            r.setVip(false);
        }
        return r;
    }

    public boolean isUserVip(Long userId) {
        return vipRepository.findActiveByUserId(userId, LocalDateTime.now()).isPresent();
    }

    @Transactional
    public VipStatusResponse purchaseVip(VipPurchaseRequest request) {
        String plan = request.getPlan() != null ? request.getPlan().toUpperCase() : "MONTHLY";
        Integer price = VIP_PRICES.get(plan);
        if (price == null) {
            throw new BadRequestException("Invalid VIP plan: " + plan + ". Use MONTHLY or YEARLY");
        }

        UserEntity user = getCurrentUser();

        // Check if user already has active VIP
        if (isUserVip(user.getId())) {
            throw new BadRequestException("You already have an active VIP subscription");
        }

        // Lock wallet
        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase() : "COIN";
        int newBalance;

        if ("POINT".equalsIgnoreCase(currency)) {
            if (wallet.getPointBalance() < price) {
                throw new BadRequestException("Insufficient point balance");
            }
            wallet.setPointBalance(wallet.getPointBalance() - price);
            newBalance = wallet.getPointBalance();
        } else {
            if (wallet.getCoinBalance() < price) {
                throw new BadRequestException("Insufficient coin balance");
            }
            wallet.setCoinBalance(wallet.getCoinBalance() - price);
            newBalance = wallet.getCoinBalance();
        }
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create subscription
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = "YEARLY".equals(plan)
                ? now.plusYears(1)
                : now.plusMonths(1);

        VipSubscriptionEntity sub = new VipSubscriptionEntity();
        sub.setUser(user);
        sub.setPlan(plan);
        sub.setStartDate(now);
        sub.setEndDate(endDate);
        sub.setStatus("ACTIVE");
        vipRepository.save(sub);

        // Record transaction
        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setUser(user);
        tx.setType(TransactionType.VIP_PURCHASE);
        tx.setAmount(price);
        tx.setCurrency(currency);
        tx.setBalanceAfter(newBalance);
        tx.setDescription("VIP " + plan + " subscription");
        tx.setReferenceId("vip-" + sub.getId());
        transactionRepository.save(tx);

        VipStatusResponse r = new VipStatusResponse();
        r.setVip(true);
        r.setPlan(plan);
        r.setStartDate(now.toString());
        r.setEndDate(endDate.toString());
        r.setStatus("ACTIVE");
        return r;
    }

    // ── Ad Reward ───────────────────────────────────────

    @Transactional
    public WalletResponse rewardAd(AdRewardRequest request) {
        UserEntity user = getCurrentUser();

        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        int reward = AD_REWARD_COINS;
        String currency = (request != null && "POINT".equalsIgnoreCase(request.getRewardType()))
                ? "POINT" : "COIN";
        int newBalance;

        if ("POINT".equals(currency)) {
            wallet.setPointBalance(wallet.getPointBalance() + reward);
            newBalance = wallet.getPointBalance();
        } else {
            wallet.setCoinBalance(wallet.getCoinBalance() + reward);
            newBalance = wallet.getCoinBalance();
        }
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setUser(user);
        tx.setType(TransactionType.AD_REWARD);
        tx.setAmount(reward);
        tx.setCurrency(currency);
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Ad reward +" + reward + " " + currency);
        tx.setReferenceId(request != null && request.getAdUnitId() != null
                ? "ad-" + request.getAdUnitId() : "ad-reward");
        transactionRepository.save(tx);

        return toWalletResponse(wallet);
    }

    // ── IAP Verification ────────────────────────────────

    @Transactional
    public WalletResponse verifyIap(IapVerifyRequest request) {
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw new BadRequestException("productId is required");
        }
        if (request.getPurchaseToken() == null || request.getPurchaseToken().isBlank()) {
            throw new BadRequestException("purchaseToken is required");
        }

        Integer coinAmount = IAP_PRODUCTS.get(request.getProductId());
        if (coinAmount == null) {
            throw new BadRequestException("Unknown product: " + request.getProductId());
        }

        // In production, verify the purchaseToken with Google/Apple servers here.
        // For now we trust the client and credit the coins.

        UserEntity user = getCurrentUser();

        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        wallet.setCoinBalance(wallet.getCoinBalance() + coinAmount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setUser(user);
        tx.setType(TransactionType.TOP_UP);
        tx.setAmount(coinAmount);
        tx.setCurrency("COIN");
        tx.setBalanceAfter(wallet.getCoinBalance());
        tx.setDescription("IAP " + request.getProductId() + " (" + request.getStore() + ")");
        tx.setReferenceId(request.getOrderId() != null ? request.getOrderId() : request.getPurchaseToken());
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
