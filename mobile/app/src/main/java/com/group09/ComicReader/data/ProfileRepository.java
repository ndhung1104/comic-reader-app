package com.group09.ComicReader.data;

import com.group09.ComicReader.R;
import com.group09.ComicReader.model.ProfileMenuItem;

import java.util.ArrayList;
import java.util.Arrays;
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

    public List<ProfileMenuItem> getMenuItems() {
        return new ArrayList<>(Arrays.asList(
                new ProfileMenuItem("Account Details", "", R.drawable.ic_nav_profile, false),
                new ProfileMenuItem("App Settings", "", R.drawable.ic_filter_24, false),
                new ProfileMenuItem("Dark Mode", "ON", R.drawable.ic_nav_home, false),
                new ProfileMenuItem("Language", "English", R.drawable.ic_nav_search, false),
                new ProfileMenuItem("Wallet and Coins", "", R.drawable.ic_wallet_24, true),
                new ProfileMenuItem("Clear Cache", "", R.drawable.ic_close_24, false)
        ));
    }
}
