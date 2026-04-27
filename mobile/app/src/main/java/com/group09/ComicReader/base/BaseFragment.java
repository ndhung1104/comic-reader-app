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

    protected void saveCsvToDownloads(String filename, String csvData) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename);
            values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = requireContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (java.io.OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        outputStream.write(csvData.getBytes());
                        showToast("Saved to Downloads: " + filename);
                        openFileIntent(uri);
                    }
                } catch (java.io.IOException e) {
                    showToast("Failed to save file: " + e.getMessage());
                }
            }
        } else {
            // Legacy way for older Android
            java.io.File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(path, filename);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(csvData.getBytes());
                showToast("Saved to Downloads: " + filename);
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", file);
                openFileIntent(uri);
            } catch (java.io.IOException e) {
                showToast("Failed to save file: " + e.getMessage());
            }
        }
    }

    private void openFileIntent(android.net.Uri uri) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/plain");
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(android.content.Intent.createChooser(intent, "Open Report"));
        } catch (android.content.ActivityNotFoundException e) {
            showToast("No app found to open text files.");
        }
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
