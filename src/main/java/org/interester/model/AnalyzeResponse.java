package org.interester.model;

import lombok.With;

import java.util.List;

public record AnalyzeResponse(
        String status,
        @With List<String> summary,
        Query query,
        List<TopicResult> results,
        List<RankItem> ranking,
        List<String> notes,
        @With ReportFiles files) {

    public static AnalyzeResponse ok(Query query, List<TopicResult> results, List<RankItem> ranking, List<String> notes) {
        return new AnalyzeResponse("ok", null, query, results, ranking, notes, null);
    }

    public record Query(List<String> topics, List<String> langs, String from, String to, int months) {
    }

    public record RankItem(int rank, String topic, String lang, Double growthVsWikiPct, Confidence confidence) {
    }

    public record ReportFiles(String chart, String pdf) {
    }
}
