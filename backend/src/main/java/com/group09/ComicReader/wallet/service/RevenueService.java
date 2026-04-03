package com.group09.ComicReader.wallet.service;

import com.group09.ComicReader.wallet.dto.DailyRevenueResponse;
import com.group09.ComicReader.wallet.dto.RevenueSummaryResponse;
import com.group09.ComicReader.wallet.entity.TransactionType;
import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import com.group09.ComicReader.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RevenueService {

    private final WalletTransactionRepository transactionRepository;

    public RevenueService(WalletTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public RevenueSummaryResponse getSummary(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Long totalTopUp = transactionRepository.sumAmountByTypeAndDateRange(TransactionType.TOP_UP, start, end);
        Long totalPurchase = transactionRepository.sumAmountByTypeAndDateRange(TransactionType.PURCHASE, start, end);
        Long totalVip = transactionRepository.sumAmountByTypeAndDateRange(TransactionType.VIP_PURCHASE, start, end);
        Long count = transactionRepository.countByDateRange(start, end);

        RevenueSummaryResponse response = new RevenueSummaryResponse();
        response.setTotalTopUp(totalTopUp);
        response.setTotalPurchase(totalPurchase);
        response.setTotalVip(totalVip);
        response.setTotalRevenue(totalTopUp + totalPurchase + totalVip);
        response.setTransactionCount(count);
        response.setFrom(from);
        response.setTo(to);
        return response;
    }

    public List<DailyRevenueResponse> getDailyRevenue(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<WalletTransactionEntity> transactions = transactionRepository.findAllByDateRange(start, end);

        // Group by date
        Map<LocalDate, DailyRevenueResponse> dailyMap = new LinkedHashMap<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            DailyRevenueResponse day = new DailyRevenueResponse();
            day.setDate(current);
            day.setTopUp(0L);
            day.setPurchase(0L);
            day.setVip(0L);
            day.setTotal(0L);
            day.setCount(0L);
            dailyMap.put(current, day);
            current = current.plusDays(1);
        }

        for (WalletTransactionEntity tx : transactions) {
            LocalDate txDate = tx.getCreatedAt().toLocalDate();
            DailyRevenueResponse day = dailyMap.get(txDate);
            if (day == null) continue;

            long amount = tx.getAmount() != null ? tx.getAmount().longValue() : 0L;
            day.setCount(day.getCount() + 1);

            switch (tx.getType()) {
                case TOP_UP -> day.setTopUp(day.getTopUp() + amount);
                case PURCHASE -> day.setPurchase(day.getPurchase() + amount);
                case VIP_PURCHASE -> day.setVip(day.getVip() + amount);
                default -> { /* AD_REWARD, REFUND not counted as revenue */ }
            }
            day.setTotal(day.getTopUp() + day.getPurchase() + day.getVip());
        }

        return new ArrayList<>(dailyMap.values());
    }
}
