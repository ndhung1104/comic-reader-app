package com.group09.ComicReader.wallet.dto;

public class AdRewardRequest {

    private String adProvider;   // e.g. "admob", "unity"
    private String adUnitId;     // ad unit identifier
    private String rewardType;   // COIN or POINT (defaults to COIN)
    private Integer rewardAmount; // override amount (optional, server decides if null)
    private String rewardId;     // client-side idempotency key per completed ad
    private String placement;    // wallet, reader_paywall, etc.

    public String getAdProvider() { return adProvider; }
    public void setAdProvider(String adProvider) { this.adProvider = adProvider; }

    public String getAdUnitId() { return adUnitId; }
    public void setAdUnitId(String adUnitId) { this.adUnitId = adUnitId; }

    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }

    public Integer getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(Integer rewardAmount) { this.rewardAmount = rewardAmount; }

    public String getRewardId() { return rewardId; }
    public void setRewardId(String rewardId) { this.rewardId = rewardId; }

    public String getPlacement() { return placement; }
    public void setPlacement(String placement) { this.placement = placement; }
}
