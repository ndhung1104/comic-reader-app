package com.group09.ComicReader.share.controller;

import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.service.ComicService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShareController {

    private static final String APP_SCHEME = "comicreader";
    private static final String APP_NAME = "ComicReader";
    private static final String APP_PACKAGE = "com.group09.ComicReader";

    private final ComicService comicService;
    private final ChapterService chapterService;

    public ShareController(ComicService comicService, ChapterService chapterService) {
        this.comicService = comicService;
        this.chapterService = chapterService;
    }

    @GetMapping(value = "/share/comic/{comicId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareComic(@PathVariable Long comicId, HttpServletRequest request) {
        ComicResponse comic = comicService.getComic(comicId);

        String shareUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUriString();
        String appUrl = APP_SCHEME + "://comic/" + comicId;

        String title = nullToEmpty(comic.getTitle());
        String description = safeDescription(comic.getSynopsis());
        String imageUrl = toAbsoluteUrl(request, comic.getCoverUrl());

        String html = buildHtmlPage(
                shareUrl,
                appUrl,
                title,
                description,
                imageUrl,
                "Open comic in app");

        return ResponseEntity.ok(html);
    }

    @GetMapping(value = "/share/chapter/{chapterId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareChapter(@PathVariable Long chapterId, HttpServletRequest request) {
        ChapterResponse chapter = chapterService.getChapter(chapterId);
        ComicResponse comic = comicService.getComic(chapter.getComicId());

        String shareUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUriString();
        String appUrl = APP_SCHEME + "://chapter/" + chapterId
                + "?comicId=" + chapter.getComicId()
                + "&chapterNumber=" + nullToZero(chapter.getChapterNumber());

        String comicTitle = nullToEmpty(comic.getTitle());
        int chapterNumber = nullToZero(chapter.getChapterNumber());
        String title = chapterNumber > 0 ? comicTitle + " - Chapter " + chapterNumber : comicTitle;
        String description = "Read " + comicTitle + (chapterNumber > 0 ? (" - Chapter " + chapterNumber) : "") + ".";
        String imageUrl = toAbsoluteUrl(request, comic.getCoverUrl());

        String html = buildHtmlPage(
                shareUrl,
                appUrl,
                title,
                description,
                imageUrl,
                "Open chapter in app");

        return ResponseEntity.ok(html);
    }

    private static String buildHtmlPage(
            String shareUrl,
            String appUrl,
            String title,
            String description,
            String imageUrl,
            String actionLabel) {

        String safeTitle = escapeHtml(title);
        String safeDescription = escapeHtml(description);
        String safeShareUrl = escapeHtml(shareUrl);
        String safeAppUrl = escapeHtml(appUrl);
        String safeImageUrl = escapeHtml(imageUrl);
        String safeActionLabel = escapeHtml(actionLabel);
        String safeAppName = escapeHtml(APP_NAME);
        String safeAppPackage = escapeHtml(APP_PACKAGE);
        String jsAppUrl = escapeJsString(appUrl);
        String imageBlock = safeImageUrl.isEmpty()
                ? ""
                : "      <img src=\"" + safeImageUrl + "\" alt=\"" + safeTitle + "\" style=\"display:block;width:100%;max-height:320px;object-fit:cover;border-radius:18px;margin:0 0 20px 0;\"/>\n";

        return "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\"/>\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n"
                + "  <title>" + safeTitle + "</title>\n"
                + "  <meta name=\"description\" content=\"" + safeDescription + "\"/>\n"
                + "  <meta property=\"og:type\" content=\"website\"/>\n"
                + "  <meta property=\"og:site_name\" content=\"" + safeAppName + "\"/>\n"
                + "  <meta property=\"og:title\" content=\"" + safeTitle + "\"/>\n"
                + "  <meta property=\"og:description\" content=\"" + safeDescription + "\"/>\n"
                + "  <meta property=\"og:url\" content=\"" + safeShareUrl + "\"/>\n"
                + (safeImageUrl.isEmpty() ? "" : ("  <meta property=\"og:image\" content=\"" + safeImageUrl + "\"/>\n"))
                + (safeImageUrl.isEmpty() ? "" : ("  <meta property=\"og:image:alt\" content=\"" + safeTitle + "\"/>\n"))
                + "  <meta name=\"twitter:card\" content=\"summary_large_image\"/>\n"
                + "  <meta name=\"twitter:title\" content=\"" + safeTitle + "\"/>\n"
                + "  <meta name=\"twitter:description\" content=\"" + safeDescription + "\"/>\n"
                + "  <meta name=\"twitter:url\" content=\"" + safeShareUrl + "\"/>\n"
                + (safeImageUrl.isEmpty() ? "" : ("  <meta name=\"twitter:image\" content=\"" + safeImageUrl + "\"/>\n"))
                + "  <meta property=\"al:android:url\" content=\"" + safeAppUrl + "\"/>\n"
                + "  <meta property=\"al:android:package\" content=\"" + safeAppPackage + "\"/>\n"
                + "  <meta property=\"al:android:app_name\" content=\"" + safeAppName + "\"/>\n"
                + "  <meta property=\"al:web:url\" content=\"" + safeShareUrl + "\"/>\n"
                + "  <meta property=\"al:web:should_fallback\" content=\"true\"/>\n"
                + "  <script>\n"
                + "    (function () {\n"
                + "      var appUrl = '" + jsAppUrl + "';\n"
                + "      var fallbackTimer = setTimeout(function () {\n"
                + "        var el = document.getElementById('fallback');\n"
                + "        if (el) el.style.display = 'block';\n"
                + "      }, 1200);\n"
                + "      window.location.href = appUrl;\n"
                + "      window.addEventListener('pagehide', function () { clearTimeout(fallbackTimer); });\n"
                + "    })();\n"
                + "  </script>\n"
                + "</head>\n"
                + "<body style=\"margin:0;background:#fff4ef;color:#4a2506;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;\">\n"
                + "  <main style=\"max-width:560px;margin:0 auto;padding:24px 20px 40px;\">\n"
                + "    <div style=\"background:#ffffff;border:1px solid #dba177;border-radius:24px;padding:24px;box-shadow:0 18px 40px rgba(155,63,0,0.08);\">\n"
                + "      <p style=\"margin:0 0 12px 0;color:#9b3f00;font-size:12px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;\">Shared from " + safeAppName + "</p>\n"
                + imageBlock
                + "      <h1 style=\"margin:0 0 8px 0;font-size:28px;line-height:1.15;\">" + safeTitle + "</h1>\n"
                + "      <p style=\"margin:0;color:#7f512e;font-size:15px;line-height:1.6;\">" + safeDescription + "</p>\n"
                + "      <div id=\"fallback\" style=\"display:none;margin-top:24px;\">\n"
                + "        <a href=\"" + safeAppUrl + "\" style=\"display:inline-block;background:#9b3f00;color:#fff0ea;padding:12px 18px;border-radius:999px;text-decoration:none;font-weight:700;\">" + safeActionLabel + "</a>\n"
                + "        <p style=\"margin:16px 0 8px 0;color:#7f512e;line-height:1.5;\">If the app is not installed yet, you can still preview the shared content on this page and keep the link below.</p>\n"
                + "        <p style=\"margin:0;padding:12px 14px;background:#fff4ef;border:1px solid #dba177;border-radius:16px;word-break:break-all;color:#4a2506;\">" + safeShareUrl + "</p>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "  </main>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String toAbsoluteUrl(HttpServletRequest request, String maybeRelativeUrl) {
        String url = nullToEmpty(maybeRelativeUrl);
        if (url.isEmpty()) {
            return "";
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString();
        if (url.startsWith("/")) {
            return baseUrl + url;
        }
        return baseUrl + "/" + url;
    }

    private static String safeDescription(String synopsis) {
        String s = nullToEmpty(synopsis).trim();
        if (s.isEmpty()) {
            return "Read on ComicReader.";
        }
        if (s.length() <= 200) {
            return s;
        }
        return s.substring(0, 197) + "...";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String escapeHtml(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeJsString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '"' -> sb.append("\\\"");
                case '\r' -> sb.append("\\r");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
