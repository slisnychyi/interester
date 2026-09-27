package org.interester.data;

import java.util.Map;
import java.util.Optional;

public record ArticleLinks(String qid, String resolvedEnTitle, Map<String, String> titlesBySiteId) {

    public ArticleLinks {
        titlesBySiteId = Map.copyOf(titlesBySiteId);
    }

    public Optional<String> title(String lang) {
        return Optional.ofNullable(titlesBySiteId.get(lang.replace('-', '_') + "wiki"));
    }
}
