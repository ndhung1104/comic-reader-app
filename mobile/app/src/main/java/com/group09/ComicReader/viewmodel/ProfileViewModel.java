package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group09.ComicReader.data.ProfileRepository;
import com.group09.ComicReader.model.ProfileMenuItem;

import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository = ProfileRepository.getInstance();
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<List<ProfileMenuItem>> menuItems = new MutableLiveData<>();

    public void loadData(boolean isAdmin) {
        List<ProfileMenuItem> items = profileRepository.getMenuItems();
        if (isAdmin) {
            items.add(0, new ProfileMenuItem("Admin Dashboard", "ADMIN", com.group09.ComicReader.R.drawable.ic_nav_profile, false));
        }
        menuItems.setValue(items);
    }

    public void setUserInfo(String name, String emailAddress) {
        username.setValue(name);
        email.setValue(emailAddress);
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<List<ProfileMenuItem>> getMenuItems() { return menuItems; }
}
