package com.group09.ComicReader.data.download;

import androidx.annotation.Nullable;

public final class DownloadStateMachine {

    public enum Action {
        ENQUEUE,
        PAUSE,
        RESUME,
        DELETE
    }

    private DownloadStateMachine() {
    }

    public static Action resolveAction(@Nullable DownloadStatus status) {
        if (status == null) {
            return Action.ENQUEUE;
        }
        switch (status) {
            case QUEUED:
            case DOWNLOADING:
                return Action.PAUSE;
            case PAUSED:
            case FAILED:
                return Action.RESUME;
            case COMPLETED:
                return Action.DELETE;
            default:
                return Action.ENQUEUE;
        }
    }
}
