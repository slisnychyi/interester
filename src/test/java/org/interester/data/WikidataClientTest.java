package org.interester.data;

import org.interester.cli.CliException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.interester.data.FakeFetcher.fixture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikidataClientTest {

    @Test
    void resolvesTitlesInOtherLanguages() {
        FakeFetcher http = new FakeFetcher().route("titles=Astronomy", fixture("wikidata-astronomy.json"));

        ArticleLinks links = new WikidataClient(http).resolve("Astronomy");

        assertEquals("Q333", links.qid());
        assertEquals("Astronomy", links.resolvedEnTitle());
        assertEquals(Optional.of("Астрономія"), links.title("uk"));
        assertEquals(Optional.of("Astronomia"), links.title("pl"));
        assertEquals(Optional.of("Astronomie"), links.title("cs"));
        assertEquals(Optional.empty(), links.title("xx"));
        assertTrue(http.calls.getFirst().toString().contains("titles=Astronomy"));
    }

    @Test
    void followsEnglishRedirect() {
        FakeFetcher http = new FakeFetcher().route("titles=Stargazing", fixture("wikidata-stargazing.json"));

        ArticleLinks links = new WikidataClient(http).resolve("Stargazing");

        assertEquals("Amateur astronomy", links.resolvedEnTitle());
        assertEquals(Optional.of("Аматорська астрономія"), links.title("uk"));
    }

    @Test
    void unknownTopic() {
        FakeFetcher http = new FakeFetcher().route("wikidata", fixture("wikidata-missing.json"));

        CliException e = assertThrows(CliException.class, () -> new WikidataClient(http).resolve("Zzqx nonexistent"));

        assertEquals("TOPIC_NOT_FOUND", e.code());
        assertEquals(CliException.EXIT_INVALID_ARGS, e.exitCode());
    }
}
