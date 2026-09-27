package org.interester.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FakeFetcher implements HttpFetcher {

    private final Map<String, Function<URI, Response>> routes = new LinkedHashMap<>();
    private final Deque<Response> queue = new ArrayDeque<>();
    public final List<URI> calls = new ArrayList<>();

    public FakeFetcher route(String urlPart, Response response) {
        routes.put(urlPart, uri -> response);
        return this;
    }

    public FakeFetcher route(String urlPart, Function<URI, Response> handler) {
        routes.put(urlPart, handler);
        return this;
    }

    public FakeFetcher then(Response response) {
        queue.add(response);
        return this;
    }

    @Override
    public Response get(URI uri) {
        calls.add(uri);
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        String url = uri.toString();
        return routes.entrySet().stream()
                .filter(e -> url.contains(e.getKey()))
                .findFirst()
                .map(e -> e.getValue().apply(uri))
                .orElseThrow(() -> new AssertionError("No fake route for " + url));
    }

    public int callsTo(String urlPart) {
        return (int) calls.stream().filter(u -> u.toString().contains(urlPart)).count();
    }

    public static Response ok(String body) {
        return new Response(200, body);
    }

    public static Response fixture(String name) {
        return fixture(200, name);
    }

    public static Response fixture(int status, String name) {
        try (InputStream in = FakeFetcher.class.getResourceAsStream("/fixtures/" + name)) {
            if (in == null) {
                throw new AssertionError("Missing fixture " + name);
            }
            return new Response(status, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
