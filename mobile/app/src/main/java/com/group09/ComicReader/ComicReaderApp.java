package com.group09.ComicReader;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.gms.ads.MobileAds;
import com.group09.ComicReader.ads.AdMobManager;
import com.group09.ComicReader.data.local.AppSettingsStore;

public class ComicReaderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppSettingsStore settings = new AppSettingsStore(this);
        String languageCode = settings.getLanguageCode();
        if (languageCode != null && !languageCode.trim().isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
        }

        if (AdMobManager.isConfigured(BuildConfig.ADMOB_APP_ID)) {
            MobileAds.initialize(this, initializationStatus -> {
            });
        }
    }
}
