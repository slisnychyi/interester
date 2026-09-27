package org.interester.data;

import org.interester.cli.CliException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JdkHttpFetcher implements HttpFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient client;
    private final String userAgent;

    public JdkHttpFetcher(String contact) {
        this.userAgent = "interester/1.0 (Agent Skill for Wikipedia pageview research"
                + (contact == null || contact.isBlank() ? "" : "; " + contact.strip()) + ")";
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);
        ProxyConfig.fromEnv(System.getenv()).ifPresent(proxy -> {
            builder.proxy(proxy.selector());
            proxy.authenticator().ifPresent(builder::authenticator);
        });
        this.client = builder.build();
    }

    @Override
    public Response get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (IOException e) {
            throw CliException.apiError("Cannot reach " + uri.getHost() + ": "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + ".");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw CliException.apiError("Interrupted while calling " + uri.getHost() + ".");
        }
    }
}
