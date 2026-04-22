package com.group09.ComicReader.share;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.R;

public final class ShareActionExecutor {

    private static final String PACKAGE_DISCORD = "com.discord";
    private static final String PACKAGE_FACEBOOK = "com.facebook.katana";
    private static final String PACKAGE_TELEGRAM = "org.telegram.messenger";
    private static final String PACKAGE_ZALO = "com.zing.zalo";

    private ShareActionExecutor() {
    }

    public static void copyLink(@NonNull Context context, @NonNull ShareContent content) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            Toast.makeText(context, R.string.share_copy_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clipData = ClipData.newPlainText(context.getString(R.string.share_link_label), content.getUrl());
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(context, R.string.share_link_copied, Toast.LENGTH_SHORT).show();
    }

    public static void openSystemShare(@NonNull Context context, @NonNull ShareContent content) {
        Intent sendIntent = createSendIntent(buildShareText(context, content));
        Intent chooserIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title));
        startActivitySafely(context, chooserIntent, R.string.share_target_unavailable);
    }

    public static void openFacebookShare(@NonNull Context context, @NonNull ShareContent content) {
        String shareUrl = "https://www.facebook.com/sharer/sharer.php?u="
                + Uri.encode(content.getUrl())
                + "&quote="
                + Uri.encode(buildCaption(context, content));
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl));
        if (startOptional(context, browserIntent, PACKAGE_FACEBOOK)) {
            return;
        }
        if (startOptional(context, browserIntent, null)) {
            return;
        }
        openSystemShare(context, content);
    }

    public static void openTelegramShare(@NonNull Context context, @NonNull ShareContent content) {
        String shareUrl = "https://t.me/share/url?url="
                + Uri.encode(content.getUrl())
                + "&text="
                + Uri.encode(buildCaption(context, content));
        Intent telegramIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl));
        if (startOptional(context, telegramIntent, PACKAGE_TELEGRAM)) {
            return;
        }
        if (startOptional(context, telegramIntent, null)) {
            return;
        }
        openSystemShare(context, content);
    }

    public static void openZaloShare(@NonNull Context context, @NonNull ShareContent content) {
        openPackageShare(context, content, PACKAGE_ZALO, context.getString(R.string.share_platform_zalo));
    }

    public static void openDiscordShare(@NonNull Context context, @NonNull ShareContent content) {
        openPackageShare(context, content, PACKAGE_DISCORD, context.getString(R.string.share_platform_discord));
    }

    public static void openPreviewPage(@NonNull Context context, @NonNull ShareContent content) {
        Intent previewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(content.getUrl()));
        startActivitySafely(context, previewIntent, R.string.share_preview_unavailable);
    }

    private static void openPackageShare(
            @NonNull Context context,
            @NonNull ShareContent content,
            @NonNull String packageName,
            @NonNull String platformLabel) {
        Intent sendIntent = createSendIntent(buildShareText(context, content));
        sendIntent.setPackage(packageName);
        if (canResolve(context, sendIntent)) {
            startActivitySafely(context, sendIntent, R.string.share_target_unavailable);
            return;
        }
        Toast.makeText(
                context,
                context.getString(R.string.share_target_unavailable_fallback, platformLabel),
                Toast.LENGTH_SHORT
        ).show();
        openSystemShare(context, content);
    }

    @NonNull
    private static Intent createSendIntent(@NonNull String shareText) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        return sendIntent;
    }

    @NonNull
    private static String buildCaption(@NonNull Context context, @NonNull ShareContent content) {
        return content.buildCaption(
                context.getString(R.string.share_comic_caption_short),
                context.getString(R.string.share_chapter_caption_short),
                context.getString(R.string.share_chapter_caption_short_fallback)
        );
    }

    @NonNull
    private static String buildShareText(@NonNull Context context, @NonNull ShareContent content) {
        return content.buildShareText(
                context.getString(R.string.share_comic_caption_short),
                context.getString(R.string.share_chapter_caption_short),
                context.getString(R.string.share_chapter_caption_short_fallback)
        );
    }

    private static boolean startOptional(@NonNull Context context, @NonNull Intent baseIntent, @Nullable String packageName) {
        Intent launchIntent = new Intent(baseIntent);
        if (packageName != null && !packageName.trim().isEmpty()) {
            launchIntent.setPackage(packageName);
        }
        if (!canResolve(context, launchIntent)) {
            return false;
        }
        try {
            context.startActivity(launchIntent);
            return true;
        } catch (ActivityNotFoundException ignored) {
            return false;
        }
    }

    private static void startActivitySafely(@NonNull Context context, @NonNull Intent intent, int unavailableMessageRes) {
        if (!canResolve(context, intent)) {
            Toast.makeText(context, unavailableMessageRes, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(context, unavailableMessageRes, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean canResolve(@NonNull Context context, @NonNull Intent intent) {
        return intent.resolveActivity(context.getPackageManager()) != null;
    }
}
