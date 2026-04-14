package com.group09.ComicReader.data.remote;

import android.content.Context;

import com.group09.ComicReader.BuildConfig;
import com.group09.ComicReader.data.local.SessionManager;
import com.group09.ComicReader.util.PerfLogger;
import com.group09.ComicReader.util.PerfSession;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String SCREEN_NAME = "ApiClient";

    // Emulator -> host machine
    public static final String DEFAULT_BASE_URL = normalizeBaseUrl(BuildConfig.BASE_URL);
    public static final String PUBLIC_BASE_URL = normalizeBaseUrl(BuildConfig.PUBLIC_BASE_URL);

    private final Retrofit retrofit;
    private final OkHttpClient okHttpClient;
    private final ApiRequestMetrics requestMetrics;

    public ApiClient(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        requestMetrics = new ApiRequestMetrics();

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(sessionManager))
                .addInterceptor(createNetworkPerfInterceptor())
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    public OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    public AuthApi authApi() {
        return retrofit.create(AuthApi.class);
    }

    public ComicApi comicApi() {
        return retrofit.create(ComicApi.class);
    }

    public CategoryApi categoryApi() {
        return retrofit.create(CategoryApi.class);
    }

    public ChapterApi chapterApi() {
        return retrofit.create(ChapterApi.class);
    }

    public CommentApi commentApi() {
        return retrofit.create(CommentApi.class);
    }

    public UserApi userApi() {
        return retrofit.create(UserApi.class);
    }

    public LibraryApi libraryApi() {
        return retrofit.create(LibraryApi.class);
    }

    public WalletApi walletApi() {
        return retrofit.create(WalletApi.class);
    }

    public AdminApi adminApi() {
        return retrofit.create(AdminApi.class);
    }

    public TranslateApi translateApi() {
        return retrofit.create(TranslateApi.class);
    }

    private Interceptor createNetworkPerfInterceptor() {
        return chain -> {
            Request request = chain.request();
            long startNs = PerfSession.startTimer();
            String method = request.method();
            String path = request.url().encodedPath();
            String endpoint = method + " " + path;
            try {
                Response response = chain.proceed(request);
                logNetworkEvent("http_response", endpoint, method, path, response.code(), startNs, null);
                return response;
            } catch (java.io.IOException exception) {
                logNetworkEvent("http_failure", endpoint, method, path, -1, startNs, exception);
                throw exception;
            }
        };
    }

    private void logNetworkEvent(
            String event,
            String endpoint,
            String method,
            String path,
            int status,
            long startNs,
            Exception exception) {
        long durationMs = PerfSession.durationMs(startNs);
        int requestCount = requestMetrics.increment(endpoint);
        if (exception == null) {
            PerfLogger.d(
                    PerfLogger.TAG_NET,
                    SCREEN_NAME,
                    event,
                    PerfLogger.kv("method", method),
                    PerfLogger.kv("path", path),
                    PerfLogger.kv("status", status),
                    PerfLogger.kv("durationMs", durationMs),
                    PerfLogger.kv("windowCount", requestCount));
            return;
        }
        PerfLogger.w(
                PerfLogger.TAG_NET,
                SCREEN_NAME,
                event,
                PerfLogger.kv("method", method),
                PerfLogger.kv("path", path),
                PerfLogger.kv("status", status),
                PerfLogger.kv("durationMs", durationMs),
                PerfLogger.kv("windowCount", requestCount),
                PerfLogger.kv("error", exception.getClass().getSimpleName()));
    }

    public static String toAbsoluteUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            return "";
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        String relative = pathOrUrl.startsWith("/") ? pathOrUrl.substring(1) : pathOrUrl;
        return DEFAULT_BASE_URL + relative;
    }

    public static String toAbsolutePublicUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            return "";
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        String relative = pathOrUrl.startsWith("/") ? pathOrUrl.substring(1) : pathOrUrl;
        return PUBLIC_BASE_URL + relative;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }
}
