package com.group09.ComicReader.model;

public class AdRewardRequest {
    private final String adProvider;
    private final String adUnitId;
    private final String rewardType;
    private final String rewardId;
    private final String placement;

    public AdRewardRequest(String adProvider, String adUnitId, String rewardType, String rewardId, String placement) {
        this.adProvider = adProvider;
        this.adUnitId = adUnitId;
        this.rewardType = rewardType;
        this.rewardId = rewardId;
        this.placement = placement;
    }

    public String getAdProvider() {
        return adProvider;
    }

    public String getAdUnitId() {
        return adUnitId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public String getRewardId() {
        return rewardId;
    }

    public String getPlacement() {
        return placement;
    }
}
