package com.group09.ComicReader.ui.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;

public class LanguagePickerDialog {
    public interface Listener {
        void onLanguageSelected(@NonNull String languageCode);
    }

    private static final String[] LANGUAGE_CODES = {"en", "vi"};

    public static void show(@NonNull Context context, @NonNull Listener listener) {
        show(context, listener, null);
    }

    public static void show(@NonNull Context context, @NonNull Listener listener, String currentLanguageCode) {
        final String[] languageNames = {
                context.getString(com.group09.ComicReader.R.string.profile_menu_language_en),
                context.getString(com.group09.ComicReader.R.string.profile_menu_language_vi)
        };
        int checkedIndex = 0;
        if (currentLanguageCode != null) {
            for (int i = 0; i < LANGUAGE_CODES.length; i++) {
                if (LANGUAGE_CODES[i].equals(currentLanguageCode)) {
                    checkedIndex = i;
                    break;
                }
            }
        }
        // Use dark dialog theme
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(context.getString(com.group09.ComicReader.R.string.choose_language))
                .setSingleChoiceItems(languageNames, checkedIndex, null)
                .setPositiveButton(context.getString(android.R.string.ok), (dialog, which) -> {
                    AlertDialog alert = (AlertDialog) dialog;
                    int selected = alert.getListView().getCheckedItemPosition();
                    listener.onLanguageSelected(LANGUAGE_CODES[selected]);
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            // Set button color for dark mode
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(com.group09.ComicReader.R.color.accent_primary));
        });
        dialog.show();
    }
}
