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

        return "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\"/>\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n"
                + "  <title>" + safeTitle + "</title>\n"
                + "  <meta name=\"description\" content=\"" + safeDescription + "\"/>\n"
                + "  <meta property=\"og:type\" content=\"website\"/>\n"
                + "  <meta property=\"og:title\" content=\"" + safeTitle + "\"/>\n"
                + "  <meta property=\"og:description\" content=\"" + safeDescription + "\"/>\n"
                + "  <meta property=\"og:url\" content=\"" + safeShareUrl + "\"/>\n"
                + (safeImageUrl.isEmpty() ? "" : ("  <meta property=\"og:image\" content=\"" + safeImageUrl + "\"/>\n"))
                + "  <meta name=\"twitter:card\" content=\"summary_large_image\"/>\n"
                + "  <meta name=\"twitter:title\" content=\"" + safeTitle + "\"/>\n"
                + "  <meta name=\"twitter:description\" content=\"" + safeDescription + "\"/>\n"
                + (safeImageUrl.isEmpty() ? "" : ("  <meta name=\"twitter:image\" content=\"" + safeImageUrl + "\"/>\n"))
                + "  <script>\n"
                + "    (function () {\n"
                + "      var appUrl = '" + safeAppUrl + "';\n"
                + "      var fallbackTimer = setTimeout(function () {\n"
                + "        var el = document.getElementById('fallback');\n"
                + "        if (el) el.style.display = 'block';\n"
                + "      }, 1200);\n"
                + "      window.location.href = appUrl;\n"
                + "      window.addEventListener('pagehide', function () { clearTimeout(fallbackTimer); });\n"
                + "    })();\n"
                + "  </script>\n"
                + "</head>\n"
                + "<body style=\"font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; padding: 20px;\">\n"
                + "  <h1 style=\"margin: 0 0 8px 0;\">" + safeTitle + "</h1>\n"
                + "  <p style=\"margin: 0 0 16px 0; color: #444;\">" + safeDescription + "</p>\n"
                + "  <div id=\"fallback\" style=\"display:none;\">\n"
                + "    <p style=\"margin: 16px 0;\">If the app didn't open, tap below:</p>\n"
                + "    <p><a href=\"" + safeAppUrl + "\" style=\"font-size: 16px;\">" + safeActionLabel + "</a></p>\n"
                + "  </div>\n"
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
}
