package com.group09.ComicReader.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class RewardedAdController {

    public interface Listener {
        void onRewardEarned(@NonNull RewardItem rewardItem);

        void onAdUnavailable(@NonNull String message);

        void onAdClosed();
    }

    private final String adUnitId;
    private final Context appContext;
    private RewardedAd rewardedAd;
    private boolean loading;

    public RewardedAdController(@NonNull Context context, @NonNull String adUnitId) {
        this.appContext = context.getApplicationContext();
        this.adUnitId = adUnitId;
    }

    public void preload() {
        if (loading || !AdMobManager.isConfigured(adUnitId)) {
            return;
        }

        loading = true;
        RewardedAd.load(appContext, adUnitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                loading = false;
                rewardedAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                loading = false;
                rewardedAd = null;
            }
        });
    }

    public void show(@NonNull Activity activity, @NonNull Listener listener) {
        if (!AdMobManager.isConfigured(adUnitId)) {
            listener.onAdUnavailable("Rewarded ads are not configured right now.");
            return;
        }

        RewardedAd ad = rewardedAd;
        if (ad == null) {
            preload();
            listener.onAdUnavailable("Rewarded ad is still loading. Please try again in a moment.");
            return;
        }

        rewardedAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preload();
                listener.onAdClosed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                preload();
                listener.onAdUnavailable("Failed to show rewarded ad. Please try again.");
            }
        });
        ad.show(activity, listener::onRewardEarned);
    }
}
