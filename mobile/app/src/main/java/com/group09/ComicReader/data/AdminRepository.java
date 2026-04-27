package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.AdminUserResponse;
import com.group09.ComicReader.model.CommentItem;
import com.group09.ComicReader.model.PageResponse;
import com.group09.ComicReader.model.UserProfileResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {

    public interface SimpleCallback {
        void onSuccess(String message);

        void onError(@NonNull String message);
    }

    public interface UserListCallback {
        void onSuccess(@NonNull PageResponse<AdminUserResponse> page);

        void onError(@NonNull String message);
    }

    public interface ImportListCallback {
        void onSuccess(@NonNull java.util.List<com.group09.ComicReader.model.ImportJobResponse> list);

        void onError(@NonNull String message);
    }

    public interface ImportActionCallback {
        void onSuccess(@NonNull com.group09.ComicReader.model.ImportJobResponse job);

        void onError(@NonNull String message);
    }

    public interface SummaryListCallback {
        void onSuccess(@NonNull java.util.List<com.group09.ComicReader.model.AiSummaryResponse> list);

        void onError(@NonNull String message);
    }

    private final ApiClient apiClient;

    public AdminRepository(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void getUsers(String search, int page, int size, @NonNull UserListCallback callback) {
        apiClient.adminApi().getUsers(search, page, size).enqueue(new Callback<PageResponse<AdminUserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<AdminUserResponse>> call,
                    @NonNull Response<PageResponse<AdminUserResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load users (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<AdminUserResponse>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void banUser(long userId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().banUser(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                    @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Ban failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess("User banned successfully");
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void unbanUser(long userId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().unbanUser(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                    @NonNull Response<UserProfileResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Unban failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess("User unbanned successfully");
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void hideComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().hideComment(commentId)
                .enqueue(new CommentCallback("Hide successful", "Hide failed", callback));
    }

    public void unhideComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().unhideComment(commentId)
                .enqueue(new CommentCallback("Unhide successful", "Unhide failed", callback));
    }

    public void lockComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().lockComment(commentId)
                .enqueue(new CommentCallback("Lock successful", "Lock failed", callback));
    }

    public void unlockComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().unlockComment(commentId)
                .enqueue(new CommentCallback("Unlock successful", "Unlock failed", callback));
    }

    public void deleteComment(long commentId, @NonNull SimpleCallback callback) {
        apiClient.adminApi().deleteComment(commentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError(extractErrorMessage(response, "Delete failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess("Delete successful");
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    private static class CommentCallback implements Callback<CommentItem> {
        private final String successMsg;
        private final String defaultErrMsg;
        private final SimpleCallback callback;

        public CommentCallback(String successMsg, String defaultErrMsg, SimpleCallback callback) {
            this.successMsg = successMsg;
            this.defaultErrMsg = defaultErrMsg;
            this.callback = callback;
        }

        @Override
        public void onResponse(@NonNull Call<CommentItem> call, @NonNull Response<CommentItem> response) {
            if (!response.isSuccessful()) {
                callback.onError(extractErrorMessage(response, defaultErrMsg + " (" + response.code() + ")"));
                return;
            }
            callback.onSuccess(successMsg);
        }

        @Override
        public void onFailure(@NonNull Call<CommentItem> call, @NonNull Throwable t) {
            callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
        }
    }

    // ── Revenue Dashboard ────────────────────────────────

    public interface RevenueSummaryCallback {
        void onSuccess(long totalTopUp, long totalPurchase, long totalVip, long totalRevenue, long txCount);

        void onError(@NonNull String message);
    }

    public interface DailyRevenueCallback {
        void onSuccess(@NonNull java.util.List<java.util.Map<String, Object>> dailyList);

        void onError(@NonNull String message);
    }

    public void getRevenueSummary(@NonNull String from, @NonNull String to, @NonNull RevenueSummaryCallback callback) {
        apiClient.adminApi().getRevenueSummary(from, to).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                    @NonNull Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Failed to load summary (" + response.code() + ")"));
                    return;
                }
                java.util.Map<String, Object> body = response.body();
                try {
                    long totalTopUp = body.containsKey("totalTopUp") ? ((Number) body.get("totalTopUp")).longValue()
                            : 0L;
                    long totalPurchase = body.containsKey("totalPurchase")
                            ? ((Number) body.get("totalPurchase")).longValue()
                            : 0L;
                    long totalVip = body.containsKey("totalVip") ? ((Number) body.get("totalVip")).longValue() : 0L;
                    long totalRevenue = body.containsKey("totalRevenue")
                            ? ((Number) body.get("totalRevenue")).longValue()
                            : 0L;
                    long txCount = body.containsKey("transactionCount")
                            ? ((Number) body.get("transactionCount")).longValue()
                            : 0L;
                    callback.onSuccess(totalTopUp, totalPurchase, totalVip, totalRevenue, txCount);
                } catch (Exception e) {
                    callback.onError("Failed to parse revenue summary");
                }
            }

            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void getDailyRevenue(@NonNull String from, @NonNull String to, @NonNull DailyRevenueCallback callback) {
        apiClient.adminApi().getDailyRevenue(from, to)
                .enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
                    @Override
                    public void onResponse(@NonNull Call<java.util.List<java.util.Map<String, Object>>> call,
                            @NonNull Response<java.util.List<java.util.Map<String, Object>>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(extractErrorMessage(response,
                                    "Failed to load daily revenue (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<java.util.List<java.util.Map<String, Object>>> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    // ── Package Management ───────────────────────────────

    public interface PackageListCallback {
        void onSuccess(@NonNull java.util.List<java.util.Map<String, Object>> packages);

        void onError(@NonNull String message);
    }

    public interface PackageActionCallback {
        void onSuccess(@NonNull java.util.Map<String, Object> pkg);

        void onError(@NonNull String message);
    }

    public interface CreatorRequestsCallback {
        void onSuccess(@NonNull com.group09.ComicReader.model.CreatorRequestPageResponse page);

        void onError(@NonNull String message);
    }

    public interface CreatorRequestActionCallback {
        void onSuccess(@NonNull com.group09.ComicReader.model.CreatorRequestResponse resp);

        void onError(@NonNull String message);
    }

    public void getAllPackages(@NonNull PackageListCallback callback) {
        apiClient.adminApi().getAllPackages().enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.List<java.util.Map<String, Object>>> call,
                    @NonNull Response<java.util.List<java.util.Map<String, Object>>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(
                            extractErrorMessage(response, "Failed to load packages (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<java.util.List<java.util.Map<String, Object>>> call,
                    @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void createPackage(String name, int coins, String priceLabel, String bonusLabel, int sortOrder,
            @NonNull PackageActionCallback callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);
        body.put("coins", coins);
        body.put("priceLabel", priceLabel);
        if (bonusLabel != null && !bonusLabel.isEmpty()) {
            body.put("bonusLabel", bonusLabel);
        }
        body.put("sortOrder", sortOrder);

        apiClient.adminApi().createPackage(body).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                    @NonNull Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(
                            extractErrorMessage(response, "Failed to create package (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void disablePackage(long id, @NonNull PackageActionCallback callback) {
        apiClient.adminApi().disablePackage(id).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                    @NonNull Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(
                            extractErrorMessage(response, "Failed to disable package (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void enablePackage(long id, @NonNull PackageActionCallback callback) {
        apiClient.adminApi().enablePackage(id).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                    @NonNull Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(
                            extractErrorMessage(response, "Failed to enable package (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    // ── Import Comic ────────────────────────────────────────

    public interface ImportComicCallback {
        void onSuccess(@NonNull com.group09.ComicReader.model.ComicResponse response);

        void onError(@NonNull String message);
    }

    public void importComic(@NonNull String sourceUrl, @NonNull String sourceType,
            @NonNull ImportComicCallback callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("sourceUrl", sourceUrl);
        body.put("sourceType", sourceType);

        apiClient.adminApi().importComic(body).enqueue(new Callback<com.group09.ComicReader.model.ComicResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.group09.ComicReader.model.ComicResponse> call,
                    @NonNull Response<com.group09.ComicReader.model.ComicResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(extractErrorMessage(response, "Import failed (" + response.code() + ")"));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<com.group09.ComicReader.model.ComicResponse> call,
                    @NonNull Throwable t) {
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void getCreatorRequests(int page, int size, @NonNull CreatorRequestsCallback callback) {
        apiClient.adminApi().getCreatorRequests(page, size)
                .enqueue(new Callback<com.group09.ComicReader.model.CreatorRequestPageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.group09.ComicReader.model.CreatorRequestPageResponse> call,
                            @NonNull Response<com.group09.ComicReader.model.CreatorRequestPageResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(extractErrorMessage(response,
                                    "Failed to load creator requests (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.group09.ComicReader.model.CreatorRequestPageResponse> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    public void approveCreatorRequest(long id, String adminMessage, @NonNull CreatorRequestActionCallback callback) {
        apiClient.adminApi().approveCreatorRequest(id, adminMessage)
                .enqueue(new Callback<com.group09.ComicReader.model.CreatorRequestResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.group09.ComicReader.model.CreatorRequestResponse> call,
                            @NonNull Response<com.group09.ComicReader.model.CreatorRequestResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(extractErrorMessage(response, "Approve failed (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.group09.ComicReader.model.CreatorRequestResponse> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    public void denyCreatorRequest(long id, String adminMessage, @NonNull CreatorRequestActionCallback callback) {
        apiClient.adminApi().denyCreatorRequest(id, adminMessage)
                .enqueue(new Callback<com.group09.ComicReader.model.CreatorRequestResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.group09.ComicReader.model.CreatorRequestResponse> call,
                            @NonNull Response<com.group09.ComicReader.model.CreatorRequestResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(extractErrorMessage(response, "Deny failed (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }
    
                    @Override
                    public void onFailure(@NonNull Call<com.group09.ComicReader.model.CreatorRequestResponse> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    // ── Moderation ───────────────────────────────────────

    public void getPendingImports(@NonNull ImportListCallback callback) {
        apiClient.adminApi().getPendingImports()
                .enqueue(new Callback<java.util.List<com.group09.ComicReader.model.ImportJobResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<java.util.List<com.group09.ComicReader.model.ImportJobResponse>> call,
                            @NonNull Response<java.util.List<com.group09.ComicReader.model.ImportJobResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(extractErrorMessage(response,
                                    "Failed to load pending imports (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<java.util.List<com.group09.ComicReader.model.ImportJobResponse>> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    public void moderateImport(long id, String status, String reason, @NonNull ImportActionCallback callback) {
        apiClient.adminApi().moderateImport(id, status, reason)
                .enqueue(new Callback<com.group09.ComicReader.model.ImportJobResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.group09.ComicReader.model.ImportJobResponse> call,
                            @NonNull Response<com.group09.ComicReader.model.ImportJobResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(
                                    extractErrorMessage(response, "Moderation failed (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.group09.ComicReader.model.ImportJobResponse> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    public void getPendingSummaries(@NonNull SummaryListCallback callback) {
        apiClient.adminApi().getPendingSummaries()
                .enqueue(new Callback<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>> call,
                            @NonNull Response<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(extractErrorMessage(response,
                                    "Failed to load pending summaries (" + response.code() + ")"));
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<java.util.List<com.group09.ComicReader.model.AiSummaryResponse>> call,
                            @NonNull Throwable t) {
                        callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
                    }
                });
    }

    public interface ExportCallback {
        void onSuccess(String csvData);
        void onError(String message);
    }

    public void exportRevenue(String from, String to, ExportCallback callback) {
        apiClient.adminApi().exportRevenue(from, to).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                handleExportResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void exportContent(String from, String to, ExportCallback callback) {
        apiClient.adminApi().exportContent(from, to).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                handleExportResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void exportUsers(String from, String to, ExportCallback callback) {
        apiClient.adminApi().exportUsers(from, to).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                handleExportResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private void handleExportResponse(Response<okhttp3.ResponseBody> response, ExportCallback callback) {
        if (response.isSuccessful() && response.body() != null) {
            try {
                callback.onSuccess(response.body().string());
            } catch (java.io.IOException e) {
                callback.onError("Read error: " + e.getMessage());
            }
        } else {
            callback.onError("Export failed (" + response.code() + ")");
        }
    }

    @NonNull
    private static String extractErrorMessage(@NonNull Response<?> response, @NonNull String fallback) {
        try {
            if (response.errorBody() == null)
                return fallback;
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty())
                return fallback;

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
