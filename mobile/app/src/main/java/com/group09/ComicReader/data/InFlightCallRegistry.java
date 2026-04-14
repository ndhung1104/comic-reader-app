package com.group09.ComicReader.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class InFlightCallRegistry {

    private final Set<Call<?>> inFlightCalls =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull Callback<T> callback) {
        track(call);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> trackedCall, @NonNull Response<T> response) {
                untrack(trackedCall);
                callback.onResponse(trackedCall, response);
            }

            @Override
            public void onFailure(@NonNull Call<T> trackedCall, @NonNull Throwable throwable) {
                untrack(trackedCall);
                if (trackedCall.isCanceled()) {
                    return;
                }
                callback.onFailure(trackedCall, throwable);
            }
        });
    }

    void track(@NonNull Call<?> call) {
        inFlightCalls.add(call);
    }

    void untrack(@NonNull Call<?> call) {
        inFlightCalls.remove(call);
    }

    void cancelAll() {
        List<Call<?>> snapshot = new ArrayList<>(inFlightCalls);
        for (Call<?> call : snapshot) {
            if (call == null || call.isCanceled()) {
                continue;
            }
            call.cancel();
        }
        inFlightCalls.clear();
    }

    int sizeForDebug() {
        return inFlightCalls.size();
    }
}
