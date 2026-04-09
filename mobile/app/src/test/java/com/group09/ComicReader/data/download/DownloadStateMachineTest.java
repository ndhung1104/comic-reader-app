package com.group09.ComicReader.data.download;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DownloadStateMachineTest {

    @Test
    public void resolveAction_nullStatus_shouldEnqueue() {
        assertEquals(DownloadStateMachine.Action.ENQUEUE, DownloadStateMachine.resolveAction(null));
    }

    @Test
    public void resolveAction_downloading_shouldPause() {
        assertEquals(DownloadStateMachine.Action.PAUSE, DownloadStateMachine.resolveAction(DownloadStatus.DOWNLOADING));
        assertEquals(DownloadStateMachine.Action.PAUSE, DownloadStateMachine.resolveAction(DownloadStatus.QUEUED));
    }

    @Test
    public void resolveAction_pausedOrFailed_shouldResume() {
        assertEquals(DownloadStateMachine.Action.RESUME, DownloadStateMachine.resolveAction(DownloadStatus.PAUSED));
        assertEquals(DownloadStateMachine.Action.RESUME, DownloadStateMachine.resolveAction(DownloadStatus.FAILED));
    }

    @Test
    public void resolveAction_completed_shouldDelete() {
        assertEquals(DownloadStateMachine.Action.DELETE, DownloadStateMachine.resolveAction(DownloadStatus.COMPLETED));
    }
}
