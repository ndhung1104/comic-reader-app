package com.group09.ComicReader.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReaderProgressStore {

    private static final String PREFS_NAME = "comic_reader_progress";
    private static final String KEY_CHAPTER_ID_SUFFIX = "_chapter_id";
    private static final String KEY_CHAPTER_NUMBER_SUFFIX = "_chapter_number";
    private static final String KEY_PAGE_POSITION_SUFFIX = "_page_position";
    private static final String KEY_OFFSET_SUFFIX = "_offset";
    private static final String KEY_UPDATED_AT_SUFFIX = "_updated_at";

    private final SharedPreferences prefs;

    public ReaderProgressStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveProgress(int comicId, int chapterId, int chapterNumber, int pagePosition, int offset) {
        if (comicId <= 0 || chapterId <= 0 || pagePosition < 0) {
            return;
        }

        String keyPrefix = keyPrefix(comicId);
        prefs.edit()
                .putInt(keyPrefix + KEY_CHAPTER_ID_SUFFIX, chapterId)
                .putInt(keyPrefix + KEY_CHAPTER_NUMBER_SUFFIX, chapterNumber)
                .putInt(keyPrefix + KEY_PAGE_POSITION_SUFFIX, pagePosition)
                .putInt(keyPrefix + KEY_OFFSET_SUFFIX, offset)
                .putLong(keyPrefix + KEY_UPDATED_AT_SUFFIX, System.currentTimeMillis())
                .apply();
    }

    @Nullable
    public ReaderProgress getProgress(int comicId) {
        if (comicId <= 0) {
            return null;
        }

        String keyPrefix = keyPrefix(comicId);
        int chapterId = prefs.getInt(keyPrefix + KEY_CHAPTER_ID_SUFFIX, -1);
        int chapterNumber = prefs.getInt(keyPrefix + KEY_CHAPTER_NUMBER_SUFFIX, -1);
        int pagePosition = prefs.getInt(keyPrefix + KEY_PAGE_POSITION_SUFFIX, -1);
        int offset = prefs.getInt(keyPrefix + KEY_OFFSET_SUFFIX, 0);
        long updatedAt = prefs.getLong(keyPrefix + KEY_UPDATED_AT_SUFFIX, 0L);

        if (chapterId <= 0 || pagePosition < 0) {
            return null;
        }
        return new ReaderProgress(chapterId, chapterNumber, pagePosition, offset, updatedAt);
    }

    @Nullable
    public ReaderProgress getProgressForChapter(int comicId, int chapterId) {
        ReaderProgress progress = getProgress(comicId);
        if (progress == null || progress.getChapterId() != chapterId) {
            return null;
        }
        return progress;
    }

    private String keyPrefix(int comicId) {
        return "comic_" + comicId;
    }

    public static class ReaderProgress {
        private final int chapterId;
        private final int chapterNumber;
        private final int pagePosition;
        private final int offset;
        private final long updatedAt;

        ReaderProgress(int chapterId, int chapterNumber, int pagePosition, int offset, long updatedAt) {
            this.chapterId = chapterId;
            this.chapterNumber = chapterNumber;
            this.pagePosition = pagePosition;
            this.offset = offset;
            this.updatedAt = updatedAt;
        }

        public int getChapterId() {
            return chapterId;
        }

        public int getChapterNumber() {
            return chapterNumber;
        }

        public int getPagePosition() {
            return pagePosition;
        }

        public int getOffset() {
            return offset;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }
    }
}
