package com.group09.ComicReader.wallet.controller;

import com.group09.ComicReader.wallet.dto.DailyRevenueResponse;
import com.group09.ComicReader.wallet.dto.RevenueSummaryResponse;
import com.group09.ComicReader.wallet.service.RevenueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/revenue")
public class AdminRevenueController {

    private final RevenueService revenueService;

    public AdminRevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/summary")
    public ResponseEntity<RevenueSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(revenueService.getSummary(from, to));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyRevenueResponse>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(revenueService.getDailyRevenue(from, to));
    }
}
