package com.group09.ComicReader.ui.reader.audio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.group09.ComicReader.model.ReaderAudioPage;

import java.util.ArrayList;
import java.util.List;

public class ReaderAudioController {
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
        listener.onPlaybackStateChanged();
    }

    public boolean playOrResume() {
        if (playlist.isEmpty()) {
            return false;
        }

        if (player != null) {
            player.play();
            player.setSpeed(playbackSpeed);
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
            listener.onPlaybackError(ReaderAudioError.MISSING_AUDIO_URL);
            listener.onPlaybackStateChanged();
            return false;
        }

        releasePlayer();
        ReaderAudioPlayer createdPlayer = playerFactory.create();
        player = createdPlayer;
        currentIndex = index;
        try {
            createdPlayer.prepare(page.getAudioUrl(), new ReaderAudioPlayer.Listener() {
                @Override
                public void onPrepared() {
                    if (player != createdPlayer) {
                        return;
                    }
                    player.setSpeed(playbackSpeed);
                    player.play();
                    listener.onPlaybackStateChanged();
                }

                @Override
                public void onCompletion() {
                    if (player != createdPlayer) {
                        return;
                    }
                    int nextIndex = currentIndex + 1;
                    if (nextIndex < playlist.size()) {
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
                    listener.onPlaybackError(ReaderAudioError.PLAYBACK_FAILED);
                    listener.onPlaybackStateChanged();
                }
            });
            listener.onPlaybackStateChanged();
            return true;
        } catch (Exception ignored) {
            releasePlayer();
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
        player = null;
    }
}
