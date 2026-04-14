package com.group09.ComicReader.ui.reader.audio;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.util.PerfLogger;

public class MediaPlayerReaderAudioPlayer implements ReaderAudioPlayer {

    private static final String SCREEN_NAME = "MediaPlayerReaderAudioPlayer";

    @Nullable
    private MediaPlayer mediaPlayer;

    @Override
    public void prepare(@NonNull String audioUrl, @NonNull Listener listener) throws Exception {
        PerfLogger.d(
                PerfLogger.TAG_READER,
                SCREEN_NAME,
                "prepare_start");
        release();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        mediaPlayer.setDataSource(audioUrl);
        mediaPlayer.setOnPreparedListener(player -> listener.onPrepared());
        mediaPlayer.setOnCompletionListener(player -> listener.onCompletion());
        mediaPlayer.setOnErrorListener((player, what, extra) -> {
            PerfLogger.w(
                    PerfLogger.TAG_READER,
                    SCREEN_NAME,
                    "prepare_error",
                    PerfLogger.kv("what", what),
                    PerfLogger.kv("extra", extra));
            listener.onError();
            return true;
        });
        mediaPlayer.prepareAsync();
    }

    @Override
    public void play() {
        if (mediaPlayer == null) {
            return;
        }
        PerfLogger.d(PerfLogger.TAG_READER, SCREEN_NAME, "play");
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            return;
        }
        PerfLogger.d(PerfLogger.TAG_READER, SCREEN_NAME, "pause");
        mediaPlayer.pause();
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void setSpeed(float speed) {
        if (mediaPlayer == null) {
            return;
        }
        try {
            PlaybackParams params = mediaPlayer.getPlaybackParams();
            if (params == null) {
                params = new PlaybackParams();
            }
            params.setSpeed(speed);
            mediaPlayer.setPlaybackParams(params);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void release() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnErrorListener(null);
            mediaPlayer.reset();
            mediaPlayer.release();
        } catch (Exception ignored) {
        }
        PerfLogger.d(PerfLogger.TAG_READER, SCREEN_NAME, "release");
        mediaPlayer = null;
    }
}
