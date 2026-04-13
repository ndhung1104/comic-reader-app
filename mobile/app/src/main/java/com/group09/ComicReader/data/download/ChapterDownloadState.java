package com.group09.ComicReader.data.download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChapterDownloadState {

    private final long chapterId;
    @NonNull
    private final DownloadStatus status;
    private final int progress;
    @Nullable
    private final String error;
    private final long updatedAt;

    public ChapterDownloadState(long chapterId, @NonNull DownloadStatus status, int progress,
            @Nullable String error, long updatedAt) {
        this.chapterId = chapterId;
        this.status = status;
        this.progress = progress;
        this.error = error;
        this.updatedAt = updatedAt;
    }

    public long getChapterId() {
        return chapterId;
    }

    @NonNull
    public DownloadStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    @Nullable
    public String getError() {
        return error;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
