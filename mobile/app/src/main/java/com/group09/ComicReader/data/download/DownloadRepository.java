package com.group09.ComicReader.data.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.group09.ComicReader.data.local.download.ChapterDownloadEntity;
import com.group09.ComicReader.data.local.download.DownloadDao;
import com.group09.ComicReader.data.local.download.ReaderLocalDatabase;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DownloadRepository {

    public static final String INPUT_CHAPTER_ID = "input_chapter_id";
    private static final int DEFAULT_PROGRESS = 0;

    private final Context appContext;
    private final DownloadDao downloadDao;
    private final WorkManager workManager;
    private final ExecutorService ioExecutor;

    public DownloadRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.downloadDao = ReaderLocalDatabase.getInstance(appContext).downloadDao();
        this.workManager = WorkManager.getInstance(appContext);
        this.ioExecutor = Executors.newSingleThreadExecutor();
    }

    @NonNull
    public LiveData<ChapterDownloadState> observe(long chapterId) {
        return Transformations.map(downloadDao.observeChapterDownload(chapterId), this::toState);
    }

    @NonNull
    public LiveData<Set<Long>> observeCompletedChapterIds() {
        return Transformations.map(downloadDao.observeCompletedChapterIds(), chapterIds -> {
            Set<Long> result = new HashSet<>();
            if (chapterIds != null) {
                result.addAll(chapterIds);
            }
            return result;
        });
    }

    public void enqueue(long chapterId) {
        if (chapterId <= 0) {
            return;
        }

        ioExecutor.execute(() -> {
            ChapterDownloadEntity current = downloadDao.getChapterDownload(chapterId);
            int progress = current == null ? DEFAULT_PROGRESS : Math.max(0, current.progress);
            downloadDao.upsertChapterDownload(new ChapterDownloadEntity(
                    chapterId,
                    DownloadStatus.QUEUED.name(),
                    progress,
                    null,
                    System.currentTimeMillis()
            ));
        });

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ChapterDownloadWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.SECONDS)
                .setInputData(new androidx.work.Data.Builder()
                        .putLong(INPUT_CHAPTER_ID, chapterId)
                        .build())
                .build();

        workManager.enqueueUniqueWork(uniqueWorkName(chapterId), ExistingWorkPolicy.REPLACE, request);
    }

    public void pause(long chapterId) {
        if (chapterId <= 0) {
            return;
        }

        workManager.cancelUniqueWork(uniqueWorkName(chapterId));
        ioExecutor.execute(() -> {
            ChapterDownloadEntity current = downloadDao.getChapterDownload(chapterId);
            int progress = current == null ? DEFAULT_PROGRESS : current.progress;
            downloadDao.upsertChapterDownload(new ChapterDownloadEntity(
                    chapterId,
                    DownloadStatus.PAUSED.name(),
                    progress,
                    null,
                    System.currentTimeMillis()
            ));
        });
    }

    public void resume(long chapterId) {
        enqueue(chapterId);
    }

    public void delete(long chapterId) {
        if (chapterId <= 0) {
            return;
        }

        workManager.cancelUniqueWork(uniqueWorkName(chapterId));
        ioExecutor.execute(() -> {
            deleteChapterDirectory(chapterId);
            downloadDao.deleteChapterPageFiles(chapterId);
            downloadDao.deleteChapterDownload(chapterId);
        });
    }

    @NonNull
    public static String uniqueWorkName(long chapterId) {
        return "chapter-download-" + chapterId;
    }

    private void deleteChapterDirectory(long chapterId) {
        File chapterDirectory = new File(appContext.getFilesDir(), "offline/chapters/" + chapterId);
        deleteRecursively(chapterDirectory);
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
        // Best effort cleanup.
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Nullable
    private ChapterDownloadState toState(@Nullable ChapterDownloadEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ChapterDownloadState(
                entity.chapterId,
                parseStatus(entity.status),
                entity.progress,
                entity.error,
                entity.updatedAt
        );
    }

    @NonNull
    private DownloadStatus parseStatus(@Nullable String status) {
        if (status == null || status.trim().isEmpty()) {
            return DownloadStatus.QUEUED;
        }
        try {
            return DownloadStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            return DownloadStatus.QUEUED;
        }
    }
}
