package com.group09.ComicReader.model;

public class WalletResponse {
    private Long id;
    private Long userId;
    private Integer coinBalance;
    private Integer pointBalance;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getCoinBalance() {
        return coinBalance;
    }

    public Integer getPointBalance() {
        return pointBalance;
    }
}
