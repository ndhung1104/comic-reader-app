package com.group09.ComicReader.common.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ErrorParser {
    private static final Pattern ERROR_PATTERN = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    public static final String TOKEN_EXPIRED_MESSAGE = "Session expired. Please log in again.";

    private ErrorParser() {
    }

    @NonNull
    public static ParsedError parseHttpError(int statusCode, @Nullable String errorBody, @NonNull String fallbackMessage) {
        if (isTokenExpiredStatus(statusCode)) {
            return new ParsedError(AppError.TOKEN_EXPIRED, TOKEN_EXPIRED_MESSAGE);
        }

        String safeFallback = sanitizeMessage(fallbackMessage, "Unknown error");
        String messageFromBody = extractMessageFromBody(errorBody);
        String message = sanitizeMessage(messageFromBody, safeFallback);

        if (statusCode == 400) {
            return new ParsedError(AppError.BAD_REQUEST, message);
        }
        if (statusCode == 429) {
            return new ParsedError(AppError.RATE_LIMITED, message);
        }
        if (statusCode == 404) {
            return new ParsedError(AppError.NOT_FOUND, message);
        }
        if (statusCode >= 500) {
            return new ParsedError(AppError.SERVER, message);
        }
        return new ParsedError(AppError.UNKNOWN, message);
    }

    @NonNull
    public static ParsedError parseThrowable(@NonNull Throwable throwable, @NonNull String fallbackMessage) {
        String message = sanitizeMessage(throwable.getMessage(), sanitizeMessage(fallbackMessage, "Network error"));
        if (throwable instanceof UnknownHostException
                || throwable instanceof ConnectException
                || throwable instanceof SocketTimeoutException
                || throwable instanceof IOException) {
            return new ParsedError(AppError.NETWORK, message);
        }
        return new ParsedError(AppError.UNKNOWN, message);
    }

    public static boolean isTokenExpiredStatus(int statusCode) {
        return statusCode == 401 || statusCode == 403;
    }

    public static boolean isTokenExpiredMessage(@Nullable String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String normalized = message.trim().toLowerCase(Locale.US);
        return normalized.equals(TOKEN_EXPIRED_MESSAGE.toLowerCase(Locale.US))
                || normalized.contains("session expired")
                || normalized.contains("token expired");
    }

    @Nullable
    private static String extractMessageFromBody(@Nullable String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return null;
        }
        String error = extractJsonField(rawBody, ERROR_PATTERN);
        if (error != null && !error.trim().isEmpty()) {
            return error;
        }
        String message = extractJsonField(rawBody, MESSAGE_PATTERN);
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        return null;
    }

    @Nullable
    private static String extractJsonField(@NonNull String rawBody, @NonNull Pattern pattern) {
        try {
            Matcher matcher = pattern.matcher(rawBody);
            if (!matcher.find() || matcher.groupCount() < 1) {
                return null;
            }
            String value = matcher.group(1);
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return value.replace("\\\"", "\"").trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private static String sanitizeMessage(@Nullable String message, @NonNull String fallback) {
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message.trim();
    }

    public static final class ParsedError {
        private final AppError error;
        private final String message;

        ParsedError(@NonNull AppError error, @NonNull String message) {
            this.error = error;
            this.message = message;
        }

        @NonNull
        public AppError getError() {
            return error;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }
}
