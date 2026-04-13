package com.group09.ComicReader.ui.reader.audio;

import com.group09.ComicReader.model.ReaderAudioPage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReaderAudioControllerTest {

    @Test
    public void playOrResume_shouldReturnFalse_whenPlaylistEmpty() {
        FakeListener listener = new FakeListener();
        FakePlayerFactory factory = new FakePlayerFactory();
        ReaderAudioController controller = new ReaderAudioController(listener, factory::create);

        assertFalse(controller.playOrResume());
        assertEquals(0, factory.createdPlayers.size());
    }

    @Test
    public void setPlaybackSpeed_shouldApplyToCurrentPlayer() {
        FakeListener listener = new FakeListener();
        FakePlayerFactory factory = new FakePlayerFactory();
        ReaderAudioController controller = new ReaderAudioController(listener, factory::create);

        controller.setPlaylist(Arrays.asList(new ReaderAudioPage(1, "https://example.com/1.mp3", 1000)));
        assertTrue(controller.playOrResume());

        controller.setPlaybackSpeed(1.25f);

        FakePlayer player = factory.createdPlayers.get(0);
        assertEquals(1.25f, player.speed, 0.0001f);
    }

    @Test
    public void pauseAndRelease_shouldBeIdempotent() {
        FakeListener listener = new FakeListener();
        FakePlayerFactory factory = new FakePlayerFactory();
        ReaderAudioController controller = new ReaderAudioController(listener, factory::create);

        controller.setPlaylist(Arrays.asList(new ReaderAudioPage(1, "https://example.com/1.mp3", 1000)));
        assertTrue(controller.playOrResume());

        controller.pause();
        controller.pause();
        controller.release();
        controller.release();

        FakePlayer player = factory.createdPlayers.get(0);
        assertEquals(1, player.pauseCount);
        assertTrue(player.released);
    }

    @Test
    public void onCompletion_shouldAdvanceToNextAudioPage() {
        FakeListener listener = new FakeListener();
        FakePlayerFactory factory = new FakePlayerFactory();
        ReaderAudioController controller = new ReaderAudioController(listener, factory::create);

        controller.setPlaylist(Arrays.asList(
                new ReaderAudioPage(1, "https://example.com/1.mp3", 1000),
                new ReaderAudioPage(2, "https://example.com/2.mp3", 1000)
        ));
        assertTrue(controller.playOrResume());

        FakePlayer firstPlayer = factory.createdPlayers.get(0);
        firstPlayer.triggerCompletion();

        assertEquals(2, factory.createdPlayers.size());
        assertTrue(factory.createdPlayers.get(1).playCount > 0);
    }

    private static class FakeListener implements ReaderAudioController.Listener {
        int stateChanges;
        ReaderAudioError lastError;

        @Override
        public void onPlaybackStateChanged() {
            stateChanges++;
        }

        @Override
        public void onPlaybackError(ReaderAudioError error) {
            lastError = error;
        }
    }

    private static class FakePlayerFactory {
        final List<FakePlayer> createdPlayers = new ArrayList<>();

        ReaderAudioPlayer create() {
            FakePlayer player = new FakePlayer();
            createdPlayers.add(player);
            return player;
        }
    }

    private static class FakePlayer implements ReaderAudioPlayer {
        ReaderAudioPlayer.Listener listener;
        boolean playing;
        boolean released;
        float speed = 1.0f;
        int pauseCount;
        int playCount;

        @Override
        public void prepare(String audioUrl, Listener listener) {
            this.listener = listener;
            listener.onPrepared();
        }

        @Override
        public void play() {
            playing = true;
            playCount++;
        }

        @Override
        public void pause() {
            if (!playing) {
                return;
            }
            playing = false;
            pauseCount++;
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public void setSpeed(float speed) {
            this.speed = speed;
        }

        @Override
        public void release() {
            released = true;
            playing = false;
        }

        void triggerCompletion() {
            if (listener != null) {
                listener.onCompletion();
            }
        }
    }
}
