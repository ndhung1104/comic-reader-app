package com.group09.ComicReader.wallet.dto;

import java.time.LocalDate;

public class RevenueSummaryResponse {

    private Long totalTopUp;
    private Long totalPurchase;
    private Long totalVip;
    private Long totalRevenue;
    private Long transactionCount;
    private LocalDate from;
    private LocalDate to;

    public Long getTotalTopUp() { return totalTopUp; }
    public void setTotalTopUp(Long totalTopUp) { this.totalTopUp = totalTopUp; }

    public Long getTotalPurchase() { return totalPurchase; }
    public void setTotalPurchase(Long totalPurchase) { this.totalPurchase = totalPurchase; }

    public Long getTotalVip() { return totalVip; }
    public void setTotalVip(Long totalVip) { this.totalVip = totalVip; }

    public Long getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Long totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Long transactionCount) { this.transactionCount = transactionCount; }

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
}
