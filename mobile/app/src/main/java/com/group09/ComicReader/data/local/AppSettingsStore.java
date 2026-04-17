package com.group09.ComicReader.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AppSettingsStore {
    private static final String PREFS_NAME = "comic_reader_settings";

    private static final String KEY_LANGUAGE_CODE = "language_code";
    private static final String KEY_LANGUAGE_SELECTED = "language_selected";
    private static final String KEY_DARK_MODE_ENABLED = "dark_mode_enabled";

    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_ONBOARDING_STEP = "onboarding_step";
    private static final String KEY_BIRTH_DATE_ISO = "birth_date_iso";
    private static final String KEY_ALLOW_MATURE_CONTENT = "allow_mature_content";
    private static final String KEY_PREFERRED_GENRES = "preferred_genres";
    private static final String KEY_PREFERRED_ART_STYLES = "preferred_art_styles";

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

    public void setDarkModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE_ENABLED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    /** 0 = language, 1 = dob, 2 = interests */
    public void setOnboardingStep(int step) {
        prefs.edit().putInt(KEY_ONBOARDING_STEP, step).apply();
    }

    public int getOnboardingStep() {
        return prefs.getInt(KEY_ONBOARDING_STEP, 0);
    }

    public void setBirthDateIso(@Nullable String isoDate) {
        prefs.edit().putString(KEY_BIRTH_DATE_ISO, isoDate).apply();
    }

    @Nullable
    public String getBirthDateIso() {
        return prefs.getString(KEY_BIRTH_DATE_ISO, null);
    }

    public void setAllowMatureContent(boolean allow) {
        prefs.edit().putBoolean(KEY_ALLOW_MATURE_CONTENT, allow).apply();
    }

    public boolean isAllowMatureContent() {
        boolean allow = prefs.getBoolean(KEY_ALLOW_MATURE_CONTENT, true);
        if (allow) {
            return true;
        }

        String dobIso = getBirthDateIso();
        if (dobIso == null || dobIso.trim().isEmpty()) {
            return false;
        }

        try {
            LocalDate dob = LocalDate.parse(dobIso.trim());
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age >= 18) {
                prefs.edit().putBoolean(KEY_ALLOW_MATURE_CONTENT, true).apply();
                return true;
            }
        } catch (Exception ignored) {
            // Keep stored setting
        }

        return false;
    }

    public void setPreferredGenres(@Nullable Set<String> genres) {
        prefs.edit().putStringSet(KEY_PREFERRED_GENRES, genres == null ? null : new HashSet<>(genres)).apply();
    }

    public Set<String> getPreferredGenres() {
        Set<String> raw = prefs.getStringSet(KEY_PREFERRED_GENRES, null);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(raw);
    }

    public void setPreferredArtStyles(@Nullable Set<String> styles) {
        prefs.edit().putStringSet(KEY_PREFERRED_ART_STYLES, styles == null ? null : new HashSet<>(styles)).apply();
    }

    public Set<String> getPreferredArtStyles() {
        Set<String> raw = prefs.getStringSet(KEY_PREFERRED_ART_STYLES, null);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(raw);
    }
}
