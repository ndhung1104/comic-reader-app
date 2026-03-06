package com.group09.ComicReader.wallet.dto;

public class WalletResponse {

    private Long id;
    private Long userId;
    private Integer coinBalance;
    private Integer pointBalance;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getCoinBalance() {
        return coinBalance;
    }

    public void setCoinBalance(Integer coinBalance) {
        this.coinBalance = coinBalance;
    }

    public Integer getPointBalance() {
        return pointBalance;
    }

    public void setPointBalance(Integer pointBalance) {
        this.pointBalance = pointBalance;
    }
}
