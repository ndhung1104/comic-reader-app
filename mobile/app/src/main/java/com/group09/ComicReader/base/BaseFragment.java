package com.group09.ComicReader.base;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    protected void showToast(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    protected void hideKeyboard() {
        if (getView() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }
}
