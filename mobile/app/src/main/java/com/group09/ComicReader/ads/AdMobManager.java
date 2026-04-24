package com.group09.ComicReader.ads;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public final class AdMobManager {

    private AdMobManager() {
    }

    public static boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static void loadBanner(@NonNull AdView adView, @NonNull String adUnitId) {
        if (!isConfigured(adUnitId)) {
            adView.setVisibility(android.view.View.GONE);
            return;
        }

        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(adUnitId);
        adView.loadAd(new AdRequest.Builder().build());
    }
}
