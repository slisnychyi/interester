package org.interester.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.interester.cli.CliException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.TreeMap;

@RequiredArgsConstructor
public class PageviewsClient {

    private static final String API = "https://wikimedia.org/api/rest_v1/metrics/pageviews";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final HttpFetcher http;

    public MonthlyViews article(String lang, String title, YearMonth from, YearMonth to) {
        String path = "/per-article/" + project(lang) + "/all-access/user/" + encodeTitle(title) + "/monthly/"
                + range(from, to);
        return fetch(path).orElseGet(MonthlyViews::empty);
    }

    public Optional<MonthlyViews> languageTotal(String lang, YearMonth from, YearMonth to) {
        return fetch("/aggregate/" + project(lang) + "/all-access/user/monthly/" + range(from, to));
    }

    private Optional<MonthlyViews> fetch(String path) {
        HttpFetcher.Response response = http.get(URI.create(API + path));
        if (response.status() == 404) {
            return Optional.empty();
        }
        if (response.status() != 200) {
            throw CliException.apiError("Pageviews API returned HTTP " + response.status() + ".");
        }
        TreeMap<YearMonth, Long> views = new TreeMap<>();
        for (JsonNode item : response.json("Pageviews API").path("items")) {
            views.merge(month(item.path("timestamp").asText()), item.path("views").asLong(), Long::sum);
        }
        return Optional.of(new MonthlyViews(views));
    }

    private static YearMonth month(String timestamp) {
        return YearMonth.parse(timestamp.substring(0, 6), MONTH);
    }

    private static String project(String lang) {
        return lang + ".wikipedia";
    }

    static String encodeTitle(String title) {
        return URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String range(YearMonth from, YearMonth to) {
        return from.atDay(1).format(DAY) + "00/" + to.atEndOfMonth().format(DAY) + "00";
    }
}
