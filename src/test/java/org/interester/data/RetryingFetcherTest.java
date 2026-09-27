package org.interester.data;

import org.interester.cli.CliException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryingFetcherTest {

    private static final URI URI_ = URI.create("https://wikimedia.org/x");
    private static final HttpFetcher.Response RATE_LIMITED =
            new HttpFetcher.Response(429, "You are making too many requests to the API.");

    private final List<Duration> pauses = new ArrayList<>();

    @Test
    void retriesRateLimitThenSucceeds() {
        FakeFetcher http = new FakeFetcher().then(RATE_LIMITED).then(RATE_LIMITED).then(FakeFetcher.ok("{}"));

        HttpFetcher.Response r = new RetryingFetcher(http, pauses::add).get(URI_);

        assertEquals(200, r.status());
        assertEquals(List.of(Duration.ofSeconds(2), Duration.ofSeconds(4)), pauses);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        FakeFetcher http = new FakeFetcher().route("wikimedia", new HttpFetcher.Response(503, "down"));

        CliException e = assertThrows(CliException.class, () -> new RetryingFetcher(http, pauses::add).get(URI_));

        assertEquals("API_ERROR", e.code());
        assertEquals(CliException.EXIT_API_ERROR, e.exitCode());
        assertEquals(RetryingFetcher.MAX_ATTEMPTS, http.calls.size());
    }

    @Test
    void doesNotRetryNotFound() {
        FakeFetcher http = new FakeFetcher().route("wikimedia", new HttpFetcher.Response(404, "{}"));

        assertEquals(404, new RetryingFetcher(http, pauses::add).get(URI_).status());
        assertEquals(1, http.calls.size());
        assertEquals(List.of(), pauses);
    }
}
