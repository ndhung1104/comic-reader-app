package com.group09.ComicReader.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wallet.ad-reward")
public class WalletRewardProperties {

    private int cooldownSeconds = 45;
    private int dailyClaimLimit = 20;
    private int coinAmount = 10;
    private int pointAmount = 5;

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public int getDailyClaimLimit() {
        return dailyClaimLimit;
    }

    public void setDailyClaimLimit(int dailyClaimLimit) {
        this.dailyClaimLimit = dailyClaimLimit;
    }

    public int getCoinAmount() {
        return coinAmount;
    }

    public void setCoinAmount(int coinAmount) {
        this.coinAmount = coinAmount;
    }

    public int getPointAmount() {
        return pointAmount;
    }

    public void setPointAmount(int pointAmount) {
        this.pointAmount = pointAmount;
    }
}
