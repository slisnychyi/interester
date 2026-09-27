package org.interester.cli;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record CliArgs(
        Command command,
        List<String> topics,
        List<String> langs,
        int months,
        YearMonth end,
        Map<String, String> manualTitles,
        Path out) {

    public YearMonth from() {
        return end.minusMonths(months - 1L);
    }
}
