package com.group09.ComicReader.data.local.download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chapter_downloads")
public class ChapterDownloadEntity {

    @PrimaryKey
    @ColumnInfo(name = "chapter_id")
    public long chapterId;

    @NonNull
    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "progress")
    public int progress;

    @Nullable
    @ColumnInfo(name = "error")
    public String error;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public ChapterDownloadEntity(long chapterId, @NonNull String status, int progress,
            @Nullable String error, long updatedAt) {
        this.chapterId = chapterId;
        this.status = status;
        this.progress = progress;
        this.error = error;
        this.updatedAt = updatedAt;
    }
}
