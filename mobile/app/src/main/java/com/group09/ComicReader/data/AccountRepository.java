package com.group09.ComicReader.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.ChangePasswordRequest;
import com.group09.ComicReader.model.UpdateProfileRequest;
import com.group09.ComicReader.model.UserProfileResponse;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountRepository {

    public interface SimpleCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    public interface MeCallback {
        void onSuccess(@NonNull UserProfileResponse me);

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public AccountRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getMe(@NonNull MeCallback callback) {
        apiClient.userApi().getMe().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to load profile (" + response.code() + ")"));
                    return;
                }
                UserProfileResponse body = response.body();
                if (body == null) {
                    callback.onError("Failed to load profile");
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void updateFullName(@NonNull String fullName, @NonNull MeCallback callback) {
        apiClient.userApi().updateMe(new UpdateProfileRequest(fullName)).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to update name (" + response.code() + ")"));
                    return;
                }
                UserProfileResponse body = response.body();
                if (body == null) {
                    callback.onError("Failed to update name");
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void changePassword(@NonNull String currentPassword, @NonNull String newPassword, @NonNull SimpleCallback callback) {
        apiClient.userApi().changePassword(new ChangePasswordRequest(currentPassword, newPassword))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                        if (!response.isSuccessful()) {
                            callback.onError(extractErrorMessage(response, "Failed to update password (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    public void updateAvatar(@NonNull Context context, @NonNull Uri imageUri, @NonNull MeCallback callback) {
        MultipartBody.Part avatarPart;
        try {
            avatarPart = createImagePartFromUri(context, imageUri, "avatar");
        } catch (Exception exception) {
            callback.onError("Failed to read selected image");
            return;
        }

        apiClient.userApi().updateAvatar(avatarPart).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Failed to update avatar (" + response.code() + ")"));
                    return;
                }
                UserProfileResponse body = response.body();
                if (body == null) {
                    callback.onError("Failed to update avatar");
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    @NonNull
    private static MultipartBody.Part createImagePartFromUri(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull String partName
    ) throws Exception {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "image/*";
        }

        String displayName = resolveDisplayName(context, uri);
        String safeName = (displayName == null || displayName.trim().isEmpty())
                ? ("avatar-" + UUID.randomUUID())
                : displayName;

        File outDir = new File(context.getCacheDir(), "uploads");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();

        File outFile = new File(outDir, safeName);

        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("Cannot open input stream");
            }
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
        }

        MediaType mediaType = MediaType.get(mimeType);
        RequestBody requestBody = RequestBody.create(outFile, mediaType);
        return MultipartBody.Part.createFormData(partName, outFile.getName(), requestBody);
    }

    private static String resolveDisplayName(@NonNull Context context, @NonNull Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) return null;
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex < 0) return null;
            if (!cursor.moveToFirst()) return null;
            return cursor.getString(nameIndex);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    private static String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        try {
            if (response.errorBody() == null) return fallback;
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) return fallback;

            JSONObject json = new JSONObject(raw);
            if (json.has("error")) {
                String message = json.optString("error", "");
                return message == null || message.trim().isEmpty() ? fallback : message;
            }
            if (json.has("message")) {
                String message = json.optString("message", "");
                return message == null || message.trim().isEmpty() ? fallback : message;
            }
            return fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
