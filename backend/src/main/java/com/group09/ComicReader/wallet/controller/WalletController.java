package com.group09.ComicReader.wallet.controller;

import com.group09.ComicReader.wallet.dto.*;
import jakarta.validation.Valid;
import com.group09.ComicReader.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        return ResponseEntity.ok(walletService.getWallet());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(Pageable pageable) {
        return ResponseEntity.ok(walletService.getTransactions(pageable));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(request));
    }

    @PostMapping("/purchase-chapter")
    public ResponseEntity<WalletResponse> purchaseChapter(@Valid @RequestBody PurchaseChapterRequest request) {
        return ResponseEntity.ok(walletService.purchaseChapter(request));
    }

    // ── VIP ────────────────────────────────────────────

    @GetMapping("/vip")
    public ResponseEntity<VipStatusResponse> getVipStatus() {
        return ResponseEntity.ok(walletService.getVipStatus());
    }

    @PostMapping("/vip/purchase")
    public ResponseEntity<VipStatusResponse> purchaseVip(@Valid @RequestBody VipPurchaseRequest request) {
        return ResponseEntity.ok(walletService.purchaseVip(request));
    }

    // ── Ad Reward ──────────────────────────────────────

    @PostMapping("/ad-reward")
    public ResponseEntity<WalletResponse> rewardAd(@RequestBody(required = false) AdRewardRequest request) {
        return ResponseEntity.ok(walletService.rewardAd(request));
    }

    // ── IAP Verification ───────────────────────────────

    @PostMapping("/iap-verify")
    public ResponseEntity<WalletResponse> verifyIap(@Valid @RequestBody IapVerifyRequest request) {
        return ResponseEntity.ok(walletService.verifyIap(request));
    }
}
