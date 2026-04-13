package com.group09.ComicReader.data.local.download;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "chapter_page_files",
        primaryKeys = {"chapter_id", "page_number"},
        indices = {@Index(value = {"chapter_id"})}
)
public class ChapterPageFileEntity {

    @ColumnInfo(name = "chapter_id")
    public long chapterId;

    @ColumnInfo(name = "page_number")
    public int pageNumber;

    @NonNull
    @ColumnInfo(name = "remote_url")
    public String remoteUrl;

    @NonNull
    @ColumnInfo(name = "local_path")
    public String localPath;

    @NonNull
    @ColumnInfo(name = "checksum")
    public String checksum;

    public ChapterPageFileEntity(long chapterId, int pageNumber, @NonNull String remoteUrl,
            @NonNull String localPath, @NonNull String checksum) {
        this.chapterId = chapterId;
        this.pageNumber = pageNumber;
        this.remoteUrl = remoteUrl;
        this.localPath = localPath;
        this.checksum = checksum;
    }
}
