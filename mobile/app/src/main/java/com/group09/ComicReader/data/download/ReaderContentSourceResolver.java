package com.group09.ComicReader.data.download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.data.local.download.ChapterDownloadEntity;
import com.group09.ComicReader.data.local.download.ChapterPageFileEntity;
import com.group09.ComicReader.model.ReaderPage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReaderContentSourceResolver {

    @NonNull
    public List<ReaderPage> resolveOfflinePages(@Nullable ChapterDownloadEntity download,
            @Nullable List<ChapterPageFileEntity> localFiles) {
        List<ReaderPage> result = new ArrayList<>();
        if (download == null || !DownloadStatus.COMPLETED.name().equals(download.status)) {
            return result;
        }
        if (localFiles == null || localFiles.isEmpty()) {
            return result;
        }

        for (ChapterPageFileEntity pageFile : localFiles) {
            if (pageFile == null || pageFile.localPath == null || pageFile.localPath.trim().isEmpty()) {
                continue;
            }
            File localFile = new File(pageFile.localPath);
            if (!localFile.exists()) {
                continue;
            }
            result.add(new ReaderPage(pageFile.pageNumber, localFile.toURI().toString()));
        }
        return result;
    }
}
