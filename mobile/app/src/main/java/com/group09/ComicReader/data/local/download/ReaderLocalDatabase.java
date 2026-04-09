package com.group09.ComicReader.data.local.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ChapterDownloadEntity.class,
                ChapterPageFileEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class ReaderLocalDatabase extends RoomDatabase {

    private static volatile ReaderLocalDatabase INSTANCE;

    public abstract DownloadDao downloadDao();

    @NonNull
    public static ReaderLocalDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ReaderLocalDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ReaderLocalDatabase.class,
                                    "reader_local.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
