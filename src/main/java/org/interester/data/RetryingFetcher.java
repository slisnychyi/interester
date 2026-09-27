package org.interester.data;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.interester.cli.CliException;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class RetryingFetcher implements HttpFetcher {

    static final int MAX_ATTEMPTS = 4;
    private static final Duration FIRST_PAUSE = Duration.ofSeconds(2);

    private final HttpFetcher inner;
    private final Consumer<Duration> sleeper;

    public RetryingFetcher(HttpFetcher inner) {
        this(inner, RetryingFetcher::sleep);
    }

    @Override
    public Response get(URI uri) {
        Duration pause = FIRST_PAUSE;
        Response response = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            response = inner.get(uri);
            if (!isRateLimitOrServerError(response.status())) {
                return response;
            }
            if (attempt < MAX_ATTEMPTS) {
                sleeper.accept(pause);
                pause = pause.multipliedBy(2);
            }
        }
        throw CliException.apiError("HTTP " + response.status() + " from " + uri.getHost()
                + " after " + MAX_ATTEMPTS + " attempts.");
    }

    private static boolean isRateLimitOrServerError(int status) {
        return status == 429 || status >= 500;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw CliException.apiError("Interrupted while waiting to retry.");
        }
    }
}
