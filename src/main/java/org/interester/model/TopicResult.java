package org.interester.model;

import lombok.Builder;

import java.util.List;

@Builder
public record TopicResult(
        String topic,
        String lang,
        String article,
        boolean found,
        Long last12,
        Long previous12,
        Double growthPct,
        Double wikiGrowthPct,
        Double growthVsWikiPct,
        Long monthlyMedian,
        Trend trend,
        List<Spike> spikes,
        String seasonalPeak,
        Confidence confidence,
        List<Reason> reasons,
        ErrorInfo error) {

    public static TopicResult notFound(String topic, String lang, ErrorInfo error) {
        return builder().topic(topic).lang(lang).found(false).error(error).build();
    }

    public record Trend(Direction direction, double slopePctPerMonth, double r2) {
    }

    public record Spike(String month, long views) {
    }

    public record Reason(String code, String text) {
    }
}
