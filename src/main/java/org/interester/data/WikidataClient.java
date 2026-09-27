package org.interester.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.interester.cli.CliException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class WikidataClient {

    private static final String API = "https://www.wikidata.org/w/api.php";

    private final HttpFetcher http;

    public ArticleLinks resolve(String enTitle) {
        URI uri = URI.create(API + "?action=wbgetentities&sites=enwiki&props=sitelinks&normalize=1&format=json&titles="
                + URLEncoder.encode(enTitle, StandardCharsets.UTF_8));
        HttpFetcher.Response response = http.get(uri);
        if (response.status() != 200) {
            throw CliException.apiError("Wikidata returned HTTP " + response.status() + ".");
        }

        JsonNode entities = response.json("Wikidata").path("entities");
        if (entities.size() != 1) {
            throw CliException.apiError("Unexpected Wikidata response for '" + enTitle + "'.");
        }
        JsonNode entity = entities.elements().next();
        if (entity.has("missing")) {
            throw CliException.topicNotFound(enTitle);
        }

        Map<String, String> titlesBySiteId = new HashMap<>();
        entity.path("sitelinks").properties()
                .forEach(e -> titlesBySiteId.put(e.getKey(), e.getValue().path("title").asText()));
        String resolvedEnTitle = titlesBySiteId.getOrDefault("enwiki", enTitle);
        return new ArticleLinks(entity.path("id").asText(), resolvedEnTitle, titlesBySiteId);
    }
}
