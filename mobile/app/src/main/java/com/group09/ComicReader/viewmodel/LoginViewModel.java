package com.group09.ComicReader.viewmodel;

import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    public boolean login(String email, String password) {
        return email != null && !email.trim().isEmpty() && password != null && !password.trim().isEmpty();
    }
}
