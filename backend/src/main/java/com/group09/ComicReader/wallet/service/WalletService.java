package com.group09.ComicReader.wallet.service;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.wallet.config.WalletRewardProperties;
import com.group09.ComicReader.wallet.dto.*;
import com.group09.ComicReader.wallet.entity.AdRewardClaimEntity;
import com.group09.ComicReader.wallet.entity.ChapterPurchaseEntity;
import com.group09.ComicReader.wallet.entity.TopUpPackageEntity;
import com.group09.ComicReader.wallet.entity.TransactionType;
import com.group09.ComicReader.wallet.entity.UserWalletEntity;
import com.group09.ComicReader.wallet.entity.VipSubscriptionEntity;
import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import com.group09.ComicReader.wallet.repository.AdRewardClaimRepository;
import com.group09.ComicReader.wallet.repository.ChapterPurchaseRepository;
import com.group09.ComicReader.wallet.repository.TopUpPackageRepository;
import com.group09.ComicReader.wallet.repository.UserWalletRepository;
import com.group09.ComicReader.wallet.repository.VipSubscriptionRepository;
import com.group09.ComicReader.wallet.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    private static final Map<String, Integer> VIP_PRICES = Map.of(
            "MONTHLY", 500,
            "YEARLY", 5000
    );

    private static final Map<String, Integer> IAP_PRODUCTS = Map.of(
            "coins_100",  100,
            "coins_500",  500,
            "coins_1000", 1000,
            "coins_2500", 2500,
            "coins_5000", 5000
    );

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AdRewardClaimRepository adRewardClaimRepository;
    private final ChapterPurchaseRepository purchaseRepository;
    private final VipSubscriptionRepository vipRepository;
    private final TopUpPackageRepository topUpPackageRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final WalletRewardProperties walletRewardProperties;

    public WalletService(UserWalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         AdRewardClaimRepository adRewardClaimRepository,
                         ChapterPurchaseRepository purchaseRepository,
                         VipSubscriptionRepository vipRepository,
                         TopUpPackageRepository topUpPackageRepository,
                         ChapterRepository chapterRepository,
                         UserRepository userRepository,
                         WalletRewardProperties walletRewardProperties) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.adRewardClaimRepository = adRewardClaimRepository;
        this.purchaseRepository = purchaseRepository;
        this.vipRepository = vipRepository;
        this.topUpPackageRepository = topUpPackageRepository;
        this.chapterRepository = chapterRepository;
        this.userRepository = userRepository;
        this.walletRewardProperties = walletRewardProperties;
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
        String currency = normalizeCurrency(request.getCurrency());
        String referenceId = normalizeReference(request.getReferenceId());

        // Lock the wallet row for update
        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        if (referenceId != null && hasExistingTransaction(user.getId(), TransactionType.TOP_UP, referenceId)) {
            LOGGER.info("wallet_topup_duplicate userId={} referenceId={}", user.getId(), referenceId);
            return toWalletResponse(wallet);
        }

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

        recordTransaction(
                user,
                TransactionType.TOP_UP,
                request.getAmount(),
                currency,
                newBalance,
                "Top-up " + request.getAmount() + " " + currency,
                referenceId
        );

        LOGGER.info("wallet_topup_success userId={} amount={} currency={} balance={} referenceId={}",
                user.getId(),
                request.getAmount(),
                currency,
                newBalance,
                referenceId);

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

        if (isUserVip(user.getId())) {
            throw new BadRequestException("Chapter already unlocked by VIP");
        }

        // Check already purchased
        if (purchaseRepository.existsByUserIdAndChapterId(user.getId(), chapter.getId())) {
            throw new BadRequestException("Chapter already purchased");
        }

        // Lock the wallet row
        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        if (purchaseRepository.existsByUserIdAndChapterId(user.getId(), chapter.getId())) {
            throw new BadRequestException("Chapter already purchased");
        }

        String currency = normalizeCurrency(request.getCurrency());
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
        try {
            purchaseRepository.save(purchase);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Chapter already purchased");
        }

        recordTransaction(
                user,
                TransactionType.PURCHASE,
                price,
                currency,
                newBalance,
                "Purchase chapter: " + chapter.getTitle(),
                buildPurchaseReference(user.getId(), chapter.getId())
        );

        LOGGER.info("wallet_purchase_success userId={} chapterId={} amount={} currency={} balance={}",
                user.getId(),
                chapter.getId(),
                price,
                currency,
                newBalance);

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

        // Lock wallet
        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        if (isUserVip(user.getId())) {
            throw new BadRequestException("You already have an active VIP subscription");
        }

        String currency = normalizeCurrency(request.getCurrency());
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

        recordTransaction(
                user,
                TransactionType.VIP_PURCHASE,
                price,
                currency,
                newBalance,
                "VIP " + plan + " subscription",
                "vip-" + sub.getId()
        );

        LOGGER.info("wallet_vip_success userId={} plan={} amount={} currency={} balance={} vipId={}",
                user.getId(),
                plan,
                price,
                currency,
                newBalance,
                sub.getId());

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
        if (request == null) {
            throw new BadRequestException("Reward request is required");
        }

        UserEntity user = getCurrentUser();
        String rewardId = normalizeReference(request.getRewardId());
        if (rewardId == null) {
            throw new BadRequestException("rewardId is required");
        }

        String placement = request.getPlacement() == null || request.getPlacement().isBlank()
                ? "wallet"
                : request.getPlacement().trim().toLowerCase();
        String currency = normalizeCurrency(request.getRewardType());

        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        Optional<AdRewardClaimEntity> existingClaim = adRewardClaimRepository.findByUserIdAndRewardId(user.getId(), rewardId);
        if (existingClaim.isPresent()) {
            LOGGER.info("wallet_ad_reward_duplicate userId={} rewardId={} placement={}", user.getId(), rewardId, placement);
            return toWalletResponse(wallet);
        }

        enforceAdRewardGuardrails(user.getId(), placement);

        int reward = resolveRewardAmount(currency);
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

        AdRewardClaimEntity claim = new AdRewardClaimEntity();
        claim.setUser(user);
        claim.setRewardId(rewardId);
        claim.setPlacement(placement);
        claim.setRewardType(currency);
        claim.setRewardAmount(reward);
        claim.setAdProvider(request.getAdProvider());
        claim.setAdUnitId(request.getAdUnitId());
        adRewardClaimRepository.save(claim);

        recordTransaction(
                user,
                TransactionType.AD_REWARD,
                reward,
                currency,
                newBalance,
                "Ad reward (" + placement + ") +" + reward + " " + currency,
                "ad-" + rewardId
        );

        LOGGER.info("wallet_ad_reward_success userId={} rewardId={} placement={} amount={} currency={} balance={} provider={}",
                user.getId(),
                rewardId,
                placement,
                reward,
                currency,
                newBalance,
                request.getAdProvider());

        return toWalletResponse(wallet);
    }

    // ── IAP Verification ────────────────────────────────

    @Transactional
    public WalletResponse verifyIap(IapVerifyRequest request) {
        UserEntity user = getCurrentUser();
        Integer coinAmount;
        String productHint;

        if (request.getPackageId() != null) {
            TopUpPackageEntity topUpPackage = topUpPackageRepository.findByIdAndActiveTrue(request.getPackageId())
                    .orElseThrow(() -> new BadRequestException("Top-up package not found or inactive"));
            coinAmount = topUpPackage.getCoins();
            productHint = "package-" + topUpPackage.getId();
        } else {
            productHint = request.getProductId();
            coinAmount = IAP_PRODUCTS.get(productHint);
            if (coinAmount == null) {
                throw new BadRequestException("Unknown product: " + productHint);
            }
        }

        String purchaseReference = request.getOrderId() != null && !request.getOrderId().isBlank()
                ? request.getOrderId()
                : request.getPurchaseToken();
        String referenceId = "iap-" + purchaseReference;

        UserWalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createWallet(user));

        Optional<WalletTransactionEntity> existing = transactionRepository
                .findFirstByUserIdAndTypeAndReferenceId(user.getId(), TransactionType.TOP_UP, referenceId);
        if (existing.isPresent()) {
            LOGGER.info("wallet_iap_duplicate userId={} referenceId={} productHint={}", user.getId(), referenceId, productHint);
            return toWalletResponse(wallet);
        }

        wallet.setCoinBalance(wallet.getCoinBalance() + coinAmount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        recordTransaction(
                user,
                TransactionType.TOP_UP,
                coinAmount,
                "COIN",
                wallet.getCoinBalance(),
                "IAP " + productHint + " (" + request.getStore() + ")",
                referenceId
        );

        LOGGER.info("wallet_iap_success userId={} referenceId={} productHint={} balance={}",
                user.getId(),
                referenceId,
                productHint,
                wallet.getCoinBalance());

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

    private void enforceAdRewardGuardrails(Long userId, String placement) {
        LocalDateTime now = LocalDateTime.now();
        adRewardClaimRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).ifPresent(lastClaim -> {
            LocalDateTime nextAllowedAt = lastClaim.getCreatedAt().plusSeconds(walletRewardProperties.getCooldownSeconds());
            if (nextAllowedAt.isAfter(now)) {
                throw new BadRequestException("Please wait before claiming another ad reward.");
            }
        });

        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long claimCount = adRewardClaimRepository.countByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
        if (claimCount >= walletRewardProperties.getDailyClaimLimit()) {
            throw new BadRequestException("Daily ad reward limit reached for today.");
        }

        LOGGER.info("wallet_ad_reward_guardrail_passed userId={} placement={} claimsToday={}", userId, placement, claimCount);
    }

    private int resolveRewardAmount(String currency) {
        if ("POINT".equals(currency)) {
            return walletRewardProperties.getPointAmount();
        }
        return walletRewardProperties.getCoinAmount();
    }

    private void recordTransaction(UserEntity user,
            TransactionType type,
            int amount,
            String currency,
            int balanceAfter,
            String description,
            String referenceId) {
        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        tx.setReferenceId(referenceId);
        transactionRepository.save(tx);
    }

    private boolean hasExistingTransaction(Long userId, TransactionType type, String referenceId) {
        if (referenceId == null) {
            return false;
        }
        return transactionRepository.findFirstByUserIdAndTypeAndReferenceId(userId, type, referenceId).isPresent();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "COIN";
        }
        return "POINT".equalsIgnoreCase(currency.trim()) ? "POINT" : "COIN";
    }

    private String normalizeReference(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            return null;
        }
        return referenceId.trim();
    }

    private String buildPurchaseReference(Long userId, Long chapterId) {
        return "chapter-" + userId + "-" + chapterId;
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
