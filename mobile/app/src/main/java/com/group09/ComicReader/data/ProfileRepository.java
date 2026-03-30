package com.group09.ComicReader.data;

import android.content.Context;
import com.group09.ComicReader.R;
import com.group09.ComicReader.model.ProfileMenuItem;
import com.group09.ComicReader.data.local.AppSettingsStore;
import java.util.ArrayList;
import java.util.List;

public class ProfileRepository {

    private static ProfileRepository instance;

    public static ProfileRepository getInstance() {
        if (instance == null) {
            instance = new ProfileRepository();
        }
        return instance;
    }

    public String getUsername() {
        return "John Doe";
    }

    public String getEmail() {
        return "john.doe@example.com";
    }

    public List<ProfileMenuItem> getMenuItems(Context context) {
        AppSettingsStore settings = new AppSettingsStore(context);
        String langCode = settings.getLanguageCode();
        String badge = "en".equals(langCode) ? context.getString(R.string.profile_menu_language_en) :
                      ("vi".equals(langCode) ? context.getString(R.string.profile_menu_language_vi) : context.getString(R.string.profile_menu_language_en));
        List<ProfileMenuItem> items = new ArrayList<>();
        items.add(new ProfileMenuItem("ACCOUNT_DETAILS", context.getString(R.string.profile_menu_account_details), "", R.drawable.ic_nav_profile, false));
        items.add(new ProfileMenuItem("APP_SETTINGS", context.getString(R.string.profile_menu_app_settings), "", R.drawable.ic_filter_24, false));
        items.add(new ProfileMenuItem("DARK_MODE", context.getString(R.string.profile_menu_dark_mode), "ON", R.drawable.ic_nav_home, false));
        items.add(new ProfileMenuItem("LANGUAGE", context.getString(R.string.profile_menu_language), badge, R.drawable.ic_nav_search, false));
        items.add(new ProfileMenuItem("WALLET", context.getString(R.string.profile_menu_wallet), "", R.drawable.ic_wallet_24, true));
        items.add(new ProfileMenuItem("CLEAR_CACHE", context.getString(R.string.profile_menu_clear_cache), "", R.drawable.ic_close_24, false));
        return items;
    }
}
