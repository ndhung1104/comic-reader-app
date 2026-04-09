package com.group09.ComicReader.data.local.download;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DownloadDao {

    @Query("SELECT * FROM chapter_downloads WHERE chapter_id = :chapterId LIMIT 1")
    ChapterDownloadEntity getChapterDownload(long chapterId);

    @Query("SELECT * FROM chapter_downloads WHERE chapter_id = :chapterId LIMIT 1")
    LiveData<ChapterDownloadEntity> observeChapterDownload(long chapterId);

    @Query("SELECT chapter_id FROM chapter_downloads WHERE status = 'COMPLETED'")
    LiveData<List<Long>> observeCompletedChapterIds();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertChapterDownload(ChapterDownloadEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertChapterPageFiles(List<ChapterPageFileEntity> entities);

    @Query("SELECT * FROM chapter_page_files WHERE chapter_id = :chapterId ORDER BY page_number ASC")
    List<ChapterPageFileEntity> getChapterPageFiles(long chapterId);

    @Query("DELETE FROM chapter_page_files WHERE chapter_id = :chapterId")
    void deleteChapterPageFiles(long chapterId);

    @Query("DELETE FROM chapter_downloads WHERE chapter_id = :chapterId")
    void deleteChapterDownload(long chapterId);
}
