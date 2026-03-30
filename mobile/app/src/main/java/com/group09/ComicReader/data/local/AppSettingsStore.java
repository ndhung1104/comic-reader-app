package com.group09.ComicReader.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class AppSettingsStore {
    private static final String PREFS_NAME = "comic_reader_settings";
    private static final String KEY_LANGUAGE_CODE = "language_code";
    private static final String KEY_LANGUAGE_SELECTED = "language_selected";

    private final SharedPreferences prefs;

    public AppSettingsStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setLanguageCode(String code) {
        prefs.edit().putString(KEY_LANGUAGE_CODE, code).apply();
    }

    @Nullable
    public String getLanguageCode() {
        return prefs.getString(KEY_LANGUAGE_CODE, null);
    }

    public void setLanguageSelected(boolean selected) {
        prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, selected).apply();
    }

    public boolean isLanguageSelected() {
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false);
    }
}
