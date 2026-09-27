package org.interester.data;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.Optional;

import static org.interester.data.FakeFetcher.fixture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageviewsClientTest {

    private static final YearMonth FROM = YearMonth.of(2024, 8);
    private static final YearMonth TO = YearMonth.of(2026, 7);

    @Test
    void articleViews() {
        FakeFetcher http = new FakeFetcher().route("per-article", fixture("pageviews-uk-astronomy.json"));

        MonthlyViews views = new PageviewsClient(http).article("uk", "Астрономія", FROM, TO);

        long[] v = views.zeroFilledValues(FROM, TO);
        assertEquals(24, v.length);
        assertEquals(16_993, Arrays.stream(v, 0, 12).sum());
        assertEquals(6_723, Arrays.stream(v, 12, 24).sum());
        assertEquals(4_687, views.views().get(YearMonth.of(2024, 9)));
        assertTrue(http.calls.getFirst().toString()
                .endsWith("/per-article/uk.wikipedia/all-access/user/%D0%90%D1%81%D1%82%D1%80%D0%BE%D0%BD%D0%BE%D0%BC%D1%96%D1%8F/monthly/2024080100/2026073100"),
                http.calls.getFirst().toString());
    }

    @Test
    void urlParts() {
        assertEquals("Intermittent_fasting", PageviewsClient.encodeTitle("Intermittent fasting"));
        assertEquals("AC%2FDC", PageviewsClient.encodeTitle("AC/DC"));
        assertEquals("%D0%90", PageviewsClient.encodeTitle("А"));
        assertEquals("2024020100/2024022900", PageviewsClient.range(YearMonth.of(2024, 2), YearMonth.of(2024, 2)));
        assertEquals("2026080100/2026083100", PageviewsClient.range(YearMonth.of(2026, 8), YearMonth.of(2026, 8)));
    }

    @Test
    void totalViewsOrEmptyForUnknownLanguage() {
        FakeFetcher http = new FakeFetcher().route("/aggregate/uk.wikipedia/", fixture("aggregate-uk.json"));

        Optional<MonthlyViews> total = new PageviewsClient(http).languageTotal("uk", FROM, TO);

        assertTrue(total.isPresent());
        long[] v = total.get().zeroFilledValues(FROM, TO);
        assertEquals(904_593_805L, Arrays.stream(v, 0, 12).sum());
        assertEquals(689_585_748L, Arrays.stream(v, 12, 24).sum());

        FakeFetcher missing = new FakeFetcher().route("aggregate", fixture(404, "pageviews-404.json"));
        assertEquals(Optional.empty(), new PageviewsClient(missing).languageTotal("xx", FROM, TO));
    }

    @Test
    void missingMonthsBecomeZero() {
        FakeFetcher http = new FakeFetcher().route("per-article",
                FakeFetcher.ok("{\"items\":[{\"timestamp\":\"2024080100\",\"views\":5},{\"timestamp\":\"2024100100\",\"views\":7}]}"));

        MonthlyViews views = new PageviewsClient(http).article("uk", "X", FROM, YearMonth.of(2024, 10));

        assertEquals(3, views.zeroFilledValues(FROM, YearMonth.of(2024, 10)).length);
        assertEquals(0, views.zeroFilledValues(FROM, YearMonth.of(2024, 10))[1]);
    }
}
