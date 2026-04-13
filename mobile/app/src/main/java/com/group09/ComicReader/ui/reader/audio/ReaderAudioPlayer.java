package com.group09.ComicReader.ui.reader.audio;

import androidx.annotation.NonNull;

public interface ReaderAudioPlayer {

    interface Listener {
        void onPrepared();

        void onCompletion();

        void onError();
    }

    void prepare(@NonNull String audioUrl, @NonNull Listener listener) throws Exception;

    void play();

    void pause();

    boolean isPlaying();

    void setSpeed(float speed);

    void release();
}
