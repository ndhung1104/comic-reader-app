package com.group09.ComicReader.wallet.dto;

import java.time.LocalDate;

public class DailyRevenueResponse {

    private LocalDate date;
    private Long topUp;
    private Long purchase;
    private Long vip;
    private Long total;
    private Long count;

    public DailyRevenueResponse() {}

    public DailyRevenueResponse(LocalDate date, Long topUp, Long purchase, Long vip, Long total, Long count) {
        this.date = date;
        this.topUp = topUp;
        this.purchase = purchase;
        this.vip = vip;
        this.total = total;
        this.count = count;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getTopUp() { return topUp; }
    public void setTopUp(Long topUp) { this.topUp = topUp; }

    public Long getPurchase() { return purchase; }
    public void setPurchase(Long purchase) { this.purchase = purchase; }

    public Long getVip() { return vip; }
    public void setVip(Long vip) { this.vip = vip; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
