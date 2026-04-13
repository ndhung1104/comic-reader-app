package com.group09.ComicReader.data.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.group09.ComicReader.data.local.download.ChapterDownloadEntity;
import com.group09.ComicReader.data.local.download.ChapterPageFileEntity;
import com.group09.ComicReader.data.local.download.DownloadDao;
import com.group09.ComicReader.data.local.download.ReaderLocalDatabase;
import com.group09.ComicReader.data.remote.ApiClient;
import com.group09.ComicReader.model.ReaderPageResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class ChapterDownloadWorker extends Worker {

    private static final int STREAM_BUFFER_SIZE = 16 * 1024;
    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final Context appContext;
    private final DownloadDao downloadDao;

    public ChapterDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.appContext = context.getApplicationContext();
        this.downloadDao = ReaderLocalDatabase.getInstance(appContext).downloadDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        long chapterId = getInputData().getLong(DownloadRepository.INPUT_CHAPTER_ID, -1L);
        if (chapterId <= 0) {
            return Result.failure();
        }

        int progress = 0;
        try {
            markState(chapterId, DownloadStatus.DOWNLOADING, progress, null);
            ApiClient apiClient = new ApiClient(appContext);

            Call<List<ReaderPageResponse>> pageCall = apiClient.chapterApi().getChapterPages(chapterId);
            retrofit2.Response<List<ReaderPageResponse>> pageResponse = pageCall.execute();
            if (!pageResponse.isSuccessful() || pageResponse.body() == null) {
                String message = "Failed to load chapter pages (" + pageResponse.code() + ")";
                markState(chapterId, DownloadStatus.FAILED, progress, message);
                return Result.failure();
            }

            List<ReaderPageResponse> pages = new ArrayList<>(pageResponse.body());
            pages.sort(Comparator.comparingInt(this::safePageNumberForSort));

            File chapterDirectory = new File(appContext.getFilesDir(), "offline/chapters/" + chapterId);
            deleteRecursively(chapterDirectory);
            if (!chapterDirectory.exists() && !chapterDirectory.mkdirs()) {
                markState(chapterId, DownloadStatus.FAILED, progress, "Cannot create offline storage");
                return Result.failure();
            }

            downloadDao.deleteChapterPageFiles(chapterId);

            if (pages.isEmpty()) {
                markState(chapterId, DownloadStatus.COMPLETED, 100, null);
                return Result.success();
            }

            List<ChapterPageFileEntity> files = new ArrayList<>();
            int total = pages.size();

            for (int index = 0; index < total; index++) {
                if (isStopped()) {
                    markState(chapterId, DownloadStatus.PAUSED, progress, null);
                    return Result.failure();
                }

                ReaderPageResponse page = pages.get(index);
                int pageNumber = safePageNumber(page, index + 1);
                String remoteUrl = ApiClient.toAbsoluteUrl(page == null ? null : page.getImageUrl());
                if (remoteUrl.trim().isEmpty()) {
                    continue;
                }

                String extension = extractExtension(remoteUrl);
                File outputFile = new File(chapterDirectory, String.format("page_%03d%s", pageNumber, extension));
                downloadToFile(apiClient, remoteUrl, outputFile);

                String checksum = sha256(outputFile);
                files.add(new ChapterPageFileEntity(
                        chapterId,
                        pageNumber,
                        remoteUrl,
                        outputFile.getAbsolutePath(),
                        checksum
                ));

                progress = Math.round(((index + 1) * 100f) / total);
                markState(chapterId, DownloadStatus.DOWNLOADING, progress, null);
                setProgressAsync(new Data.Builder().putInt("progress", progress).build());
            }

            downloadDao.upsertChapterPageFiles(files);
            markState(chapterId, DownloadStatus.COMPLETED, 100, null);
            return Result.success();
        } catch (IOException networkError) {
            if (getRunAttemptCount() < MAX_RETRY_ATTEMPTS) {
                markState(chapterId, DownloadStatus.QUEUED, progress, networkError.getMessage());
                return Result.retry();
            }
            markState(chapterId, DownloadStatus.FAILED, progress, safeMessage(networkError));
            return Result.failure();
        } catch (Exception exception) {
            markState(chapterId, DownloadStatus.FAILED, progress, safeMessage(exception));
            return Result.failure();
        }
    }

    private void markState(long chapterId, @NonNull DownloadStatus status, int progress, @Nullable String error) {
        downloadDao.upsertChapterDownload(new ChapterDownloadEntity(
                chapterId,
                status.name(),
                progress,
                error,
                System.currentTimeMillis()
        ));
    }

    private int safePageNumberForSort(@Nullable ReaderPageResponse page) {
        if (page == null || page.getPageNumber() == null || page.getPageNumber() < 1) {
            return Integer.MAX_VALUE;
        }
        return page.getPageNumber();
    }

    private int safePageNumber(@Nullable ReaderPageResponse page, int fallbackValue) {
        if (page == null || page.getPageNumber() == null || page.getPageNumber() < 1) {
            return Math.max(1, fallbackValue);
        }
        return page.getPageNumber();
    }

    private void downloadToFile(@NonNull ApiClient apiClient, @NonNull String url, @NonNull File target) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = apiClient.okHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed (" + response.code() + ")");
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }
            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(target)) {
                byte[] buffer = new byte[STREAM_BUFFER_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
        }
    }

    @NonNull
    private String sha256(@NonNull File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    @NonNull
    private String extractExtension(@NonNull String url) {
        int queryIndex = url.indexOf('?');
        String path = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int dotIndex = path.lastIndexOf('.');
        int slashIndex = path.lastIndexOf('/');
        if (dotIndex <= slashIndex || dotIndex < 0 || dotIndex == path.length() - 1) {
            return ".img";
        }
        String extension = path.substring(dotIndex).toLowerCase();
        if (extension.length() > 8) {
            return ".img";
        }
        return extension;
    }

    private void deleteRecursively(@Nullable File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Download failed";
        }
        return message;
    }
}
