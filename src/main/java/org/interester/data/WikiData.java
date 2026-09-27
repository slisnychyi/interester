package org.interester.data;

import lombok.RequiredArgsConstructor;
import org.interester.cli.CliException;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class WikiData {

    private final WikidataClient wikidata;
    private final PageviewsClient pageviews;
    private final Map<String, ArticleLinks> linksCache = new HashMap<>();
    private final Map<String, MonthlyViews> viewsCache = new HashMap<>();

    public static WikiData create() {
        HttpFetcher http = new RetryingFetcher(new JdkHttpFetcher(System.getenv("INTERESTER_CONTACT")));
        return new WikiData(new WikidataClient(http), new PageviewsClient(http));
    }

    public ArticleLinks links(String enTitle) {
        return linksCache.computeIfAbsent(enTitle, wikidata::resolve);
    }

    public MonthlyViews languageTotal(String lang, YearMonth from, YearMonth to) {
        return viewsCache.computeIfAbsent("total|" + lang + "|" + from + "|" + to,
                key -> pageviews.languageTotal(lang, from, to).orElseThrow(() -> unknownLanguage(lang)));
    }

    public MonthlyViews article(String lang, String title, YearMonth from, YearMonth to) {
        return viewsCache.computeIfAbsent("article|" + lang + "|" + title + "|" + from + "|" + to,
                key -> pageviews.article(lang, title, from, to));
    }

    private static CliException unknownLanguage(String lang) {
        return CliException.invalidArgs("There is no '" + lang + "' Wikipedia with pageview data.",
                "Check the language code. Examples: uk = Ukrainian, pl = Polish, cs = Czech, de = German.");
    }
}
