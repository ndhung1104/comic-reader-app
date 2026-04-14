package com.group09.ComicReader.data.remote;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiRequestMetricsTest {

    @Test
    public void increment_incrementsCountWithinSameWindow() {
        ApiRequestMetrics metrics = new ApiRequestMetrics(10_000L);

        int first = metrics.increment("GET /api/v1/comics", 1_000L);
        int second = metrics.increment("GET /api/v1/comics", 1_500L);

        assertEquals(1, first);
        assertEquals(2, second);
    }

    @Test
    public void increment_resetsCountWhenWindowChanges() {
        ApiRequestMetrics metrics = new ApiRequestMetrics(1_000L);

        metrics.increment("GET /api/v1/comics", 100L);
        int afterReset = metrics.increment("GET /api/v1/comics", 1_500L);

        assertEquals(1, afterReset);
    }

    @Test
    public void increment_tracksDifferentEndpointsSeparately() {
        ApiRequestMetrics metrics = new ApiRequestMetrics(10_000L);

        int firstComics = metrics.increment("GET /api/v1/comics", 1_000L);
        int firstChapters = metrics.increment("GET /api/v1/chapters", 1_100L);
        int secondComics = metrics.increment("GET /api/v1/comics", 1_200L);

        assertEquals(1, firstComics);
        assertEquals(1, firstChapters);
        assertEquals(2, secondComics);
    }
}
