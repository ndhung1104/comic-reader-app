package com.group09.ComicReader.ui.reader.audio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.model.ReaderAudioPage;
import com.group09.ComicReader.util.PerfLogger;

import java.util.ArrayList;
import java.util.List;

public class ReaderAudioController {
    private static final String SCREEN_NAME = "ReaderAudioController";

    public interface Listener {
        void onPlaybackStateChanged();

        void onPlaybackError(@NonNull ReaderAudioError error);
    }

    public interface PlayerFactory {
        @NonNull ReaderAudioPlayer create();
    }

    private static final float DEFAULT_SPEED = 1.0f;

    private final Listener listener;
    private final PlayerFactory playerFactory;
    private final List<ReaderAudioPage> playlist = new ArrayList<>();

    @Nullable
    private ReaderAudioPlayer player;

    private int currentIndex = -1;
    private float playbackSpeed = DEFAULT_SPEED;

    public ReaderAudioController(@NonNull Listener listener, @NonNull PlayerFactory playerFactory) {
        this.listener = listener;
        this.playerFactory = playerFactory;
    }

    public void setPlaylist(@Nullable List<ReaderAudioPage> pages) {
        playlist.clear();
        if (pages != null) {
            playlist.addAll(pages);
        }
        currentIndex = playlist.isEmpty() ? -1 : 0;
        releasePlayer();
        PerfLogger.d(
                PerfLogger.TAG_READER,
                SCREEN_NAME,
                "set_playlist",
                PerfLogger.kv("size", playlist.size()));
        listener.onPlaybackStateChanged();
    }

    public boolean playOrResume() {
        if (playlist.isEmpty()) {
            PerfLogger.d(PerfLogger.TAG_READER, SCREEN_NAME, "play_skipped_empty_playlist");
            return false;
        }

        if (player != null) {
            player.play();
            player.setSpeed(playbackSpeed);
            PerfLogger.d(
                    PerfLogger.TAG_READER,
                    SCREEN_NAME,
                    "resume_existing_player",
                    PerfLogger.kv("index", currentIndex));
            listener.onPlaybackStateChanged();
            return true;
        }

        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            currentIndex = 0;
        }
        return playAtIndex(currentIndex);
    }

    public void pause() {
        if (player == null || !player.isPlaying()) {
            return;
        }
        player.pause();
        PerfLogger.d(
                PerfLogger.TAG_READER,
                SCREEN_NAME,
                "pause",
                PerfLogger.kv("index", currentIndex));
        listener.onPlaybackStateChanged();
    }

    public void setPlaybackSpeed(float speed) {
        playbackSpeed = speed;
        if (player != null) {
            player.setSpeed(playbackSpeed);
        }
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean hasPlaylist() {
        return !playlist.isEmpty();
    }

    public void release() {
        PerfLogger.d(
                PerfLogger.TAG_READER,
                SCREEN_NAME,
                "release_controller",
                PerfLogger.kv("playlistSize", playlist.size()));
        releasePlayer();
        currentIndex = playlist.isEmpty() ? -1 : 0;
        listener.onPlaybackStateChanged();
    }

    private boolean playAtIndex(int index) {
        if (index < 0 || index >= playlist.size()) {
            releasePlayer();
            listener.onPlaybackStateChanged();
            return false;
        }

        ReaderAudioPage page = playlist.get(index);
        if (page.getAudioUrl() == null || page.getAudioUrl().trim().isEmpty()) {
            PerfLogger.w(
                    PerfLogger.TAG_READER,
                    SCREEN_NAME,
                    "missing_audio_url",
                    PerfLogger.kv("index", index));
            listener.onPlaybackError(ReaderAudioError.MISSING_AUDIO_URL);
            listener.onPlaybackStateChanged();
            return false;
        }

        releasePlayer();
        ReaderAudioPlayer createdPlayer = playerFactory.create();
        player = createdPlayer;
        currentIndex = index;
        PerfLogger.d(
                PerfLogger.TAG_READER,
                SCREEN_NAME,
                "prepare_player",
                PerfLogger.kv("index", index));
        try {
            createdPlayer.prepare(page.getAudioUrl(), new ReaderAudioPlayer.Listener() {
                @Override
                public void onPrepared() {
                    if (player != createdPlayer) {
                        return;
                    }
                    player.setSpeed(playbackSpeed);
                    player.play();
                    PerfLogger.d(
                            PerfLogger.TAG_READER,
                            SCREEN_NAME,
                            "prepared_and_playing",
                            PerfLogger.kv("index", currentIndex));
                    listener.onPlaybackStateChanged();
                }

                @Override
                public void onCompletion() {
                    if (player != createdPlayer) {
                        return;
                    }
                    int nextIndex = currentIndex + 1;
                    if (nextIndex < playlist.size()) {
                        PerfLogger.d(
                                PerfLogger.TAG_READER,
                                SCREEN_NAME,
                                "play_next",
                                PerfLogger.kv("nextIndex", nextIndex));
                        playAtIndex(nextIndex);
                        return;
                    }
                    currentIndex = 0;
                    releasePlayer();
                    listener.onPlaybackStateChanged();
                }

                @Override
                public void onError() {
                    if (player != createdPlayer) {
                        return;
                    }
                    releasePlayer();
                    PerfLogger.w(
                            PerfLogger.TAG_READER,
                            SCREEN_NAME,
                            "playback_error",
                            PerfLogger.kv("index", currentIndex));
                    listener.onPlaybackError(ReaderAudioError.PLAYBACK_FAILED);
                    listener.onPlaybackStateChanged();
                }
            });
            listener.onPlaybackStateChanged();
            return true;
        } catch (Exception ignored) {
            releasePlayer();
            PerfLogger.w(
                    PerfLogger.TAG_READER,
                    SCREEN_NAME,
                    "prepare_exception",
                    PerfLogger.kv("index", index));
            listener.onPlaybackError(ReaderAudioError.PLAYBACK_FAILED);
            listener.onPlaybackStateChanged();
            return false;
        }
    }

    private void releasePlayer() {
        if (player == null) {
            return;
        }
        player.release();
        PerfLogger.d(
                PerfLogger.TAG_READER,
                SCREEN_NAME,
                "release_player",
                PerfLogger.kv("index", currentIndex));
        player = null;
    }
}
