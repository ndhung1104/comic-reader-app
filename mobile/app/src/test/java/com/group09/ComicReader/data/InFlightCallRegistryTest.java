package com.group09.ComicReader.data;

import org.junit.Test;

import java.io.IOException;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InFlightCallRegistryTest {

    @Test
    public void cancelAll_cancelsAndClearsTrackedCalls() {
        InFlightCallRegistry registry = new InFlightCallRegistry();
        FakeCall first = new FakeCall();
        FakeCall second = new FakeCall();

        registry.track(first);
        registry.track(second);
        assertEquals(2, registry.sizeForDebug());

        registry.cancelAll();

        assertTrue(first.isCanceled());
        assertTrue(second.isCanceled());
        assertEquals(0, registry.sizeForDebug());
    }

    @Test
    public void untrack_removesSingleCall() {
        InFlightCallRegistry registry = new InFlightCallRegistry();
        FakeCall first = new FakeCall();
        FakeCall second = new FakeCall();

        registry.track(first);
        registry.track(second);
        registry.untrack(first);

        assertEquals(1, registry.sizeForDebug());
    }

    private static final class FakeCall implements Call<Object> {
        private boolean canceled;

        @Override
        public Response<Object> execute() throws IOException {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public void enqueue(Callback<Object> callback) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Call<Object> clone() {
            return new FakeCall();
        }

        @Override
        public Request request() {
            return new Request.Builder().url("http://localhost/").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
