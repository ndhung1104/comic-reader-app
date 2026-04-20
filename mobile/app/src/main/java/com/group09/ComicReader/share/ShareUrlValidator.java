package com.group09.ComicReader.share;

import androidx.annotation.NonNull;

import java.net.URI;
import java.util.Locale;

public final class ShareUrlValidator {

    private ShareUrlValidator() {
    }

    public static boolean needsLocalWarning(@NonNull String url) {
        if (url.trim().isEmpty()) {
            return false;
        }
        try {
            String host = URI.create(url.trim()).getHost();
            if (host == null || host.trim().isEmpty()) {
                return false;
            }
            String normalizedHost = host.trim().toLowerCase(Locale.US);
            return "localhost".equals(normalizedHost)
                    || "127.0.0.1".equals(normalizedHost)
                    || "0.0.0.0".equals(normalizedHost)
                    || "::1".equals(normalizedHost)
                    || "10.0.2.2".equals(normalizedHost)
                    || "10.0.3.2".equals(normalizedHost);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
