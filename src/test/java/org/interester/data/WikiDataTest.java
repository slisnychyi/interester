package org.interester.data;

import org.interester.cli.CliException;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.interester.data.FakeFetcher.fixture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WikiDataTest {

    private static final YearMonth FROM = YearMonth.of(2024, 8);
    private static final YearMonth TO = YearMonth.of(2026, 7);

    private final FakeFetcher http = new FakeFetcher()
            .route("wikidata", fixture("wikidata-astronomy.json"))
            .route("/aggregate/xx.", new HttpFetcher.Response(404, "{}"))
            .route("/aggregate/", fixture("aggregate-uk.json"))
            .route("/per-article/", fixture("pageviews-uk-astronomy.json"));
    private final WikiData wikiData = new WikiData(new WikidataClient(http), new PageviewsClient(http));

    @Test
    void sameRequestsGoToApiOnce() {
        wikiData.links("Astronomy");
        wikiData.links("Astronomy");
        wikiData.languageTotal("uk", FROM, TO);
        wikiData.languageTotal("uk", FROM, TO);
        wikiData.article("uk", "Астрономія", FROM, TO);
        wikiData.article("uk", "Астрономія", FROM, TO);

        assertEquals(1, http.callsTo("wikidata"));
        assertEquals(1, http.callsTo("/aggregate/"));
        assertEquals(1, http.callsTo("/per-article/"));

        wikiData.article("uk", "Астрономія", FROM.plusMonths(1), TO);
        assertEquals(2, http.callsTo("/per-article/"));
    }

    @Test
    void unknownLanguage() {
        CliException e = assertThrows(CliException.class, () -> wikiData.languageTotal("xx", FROM, TO));

        assertEquals("INVALID_ARGS", e.code());
    }
}
