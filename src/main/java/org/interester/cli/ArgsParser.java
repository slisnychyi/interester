package org.interester.cli;

import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ArgsParser {

    static final String USAGE = "Usage: analyze|report --topic <English article title> [--topic ...] "
            + "--langs <codes, e.g. pl,cs> [--months 12-120] [--end YYYY-MM] [--article lang:Title] [--out dir (report only)]";

    static final int DEFAULT_MONTHS = 24;
    static final int MIN_MONTHS = 12;
    static final int MAX_MONTHS = 120;
    static final int MAX_LANGS = 10;
    static final int MAX_PAIRS_ON_ONE_PAGE = 12;
    static final Path DEFAULT_OUT = Path.of("out");

    static final YearMonth FIRST_DATA_MONTH = YearMonth.of(2015, 7);

    private static final Pattern LANG_CODE = Pattern.compile("[a-z]{2,3}(-[a-z]+)*");

    private static final Map<String, String> COUNTRY_TO_LANGUAGE_CODE = Map.of(
            "ua", "uk", "cz", "cs", "dk", "da", "se", "sv", "gr", "el",
            "jp", "ja", "kr", "ko", "cn", "zh", "br", "pt", "ee", "et");

    private final Clock clock;

    public CliArgs parse(String[] args) {
        if (args.length == 0) {
            throw CliException.invalidArgs("No command given.", USAGE);
        }
        Command command = Command.parse(args[0]);

        List<String> topics = new ArrayList<>();
        List<String> rawManualTitles = new ArrayList<>();
        String langs = null;
        String months = null;
        String end = null;
        String out = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw CliException.invalidArgs("Unexpected value '" + arg + "'.",
                        "Put quotes around titles with spaces: --topic \"Intermittent fasting\".");
            }
            String name = arg.substring(2);
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw CliException.invalidArgs("Missing value for --" + name + ".", USAGE);
            }
            String value = args[++i].strip();
            if (value.isEmpty()) {
                throw CliException.invalidArgs("Empty value for --" + name + ".", USAGE);
            }

            switch (name) {
                case "topic" -> topics.add(value);
                case "article" -> rawManualTitles.add(value);
                case "langs" -> langs = once(name, langs, value);
                case "months" -> months = once(name, months, value);
                case "end" -> end = once(name, end, value);
                case "out" -> out = once(name, out, value);
                default -> throw CliException.invalidArgs("Unknown flag --" + name + ".", USAGE);
            }
        }

        if (topics.isEmpty()) {
            throw CliException.invalidArgs("Missing --topic.",
                    "Add --topic with the English Wikipedia article title, e.g. --topic Astronomy.");
        }
        if (langs == null) {
            throw CliException.invalidArgs("Missing --langs.",
                    "Add --langs with Wikipedia language codes, e.g. --langs uk or --langs pl,cs.");
        }
        if (out != null && command != Command.REPORT) {
            throw CliException.invalidArgs("--out is only for the report command.",
                    "Remove --out, or use the report command.");
        }

        List<String> langList = parseLangs(langs);
        int pairs = topics.size() * langList.size();
        if (command == Command.REPORT && pairs > MAX_PAIRS_ON_ONE_PAGE) {
            throw CliException.invalidArgs("Too many topic × language pairs for one report (" + pairs + ").",
                    "A report fits at most " + MAX_PAIRS_ON_ONE_PAGE + " pairs. Use fewer topics or languages, or use analyze.");
        }
        int monthCount = parseMonths(months);
        YearMonth endMonth = parseEnd(end);
        if (endMonth.minusMonths(monthCount - 1L).isBefore(FIRST_DATA_MONTH)) {
            throw CliException.invalidArgs("Period starts before " + FIRST_DATA_MONTH + ".",
                    "Wikipedia pageview data starts in " + FIRST_DATA_MONTH + ". Use fewer --months or a later --end.");
        }
        Map<String, String> manualTitles = parseManualTitles(rawManualTitles, topics, langList);

        return new CliArgs(command, List.copyOf(topics), langList, monthCount, endMonth, manualTitles,
                out == null ? DEFAULT_OUT : Path.of(out));
    }

    private static String once(String name, String previous, String value) {
        if (previous != null) {
            throw CliException.invalidArgs("--" + name + " is given twice.",
                    "Give --" + name + " once. For several languages use one list: --langs pl,cs,sk.");
        }
        return value;
    }

    private static List<String> parseLangs(String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            String lang = part.strip().toLowerCase();
            if (lang.isEmpty()) {
                continue;
            }
            String fix = COUNTRY_TO_LANGUAGE_CODE.get(lang);
            if (fix != null) {
                throw CliException.invalidArgs("Unknown language code '" + lang + "'.",
                        "Use Wikipedia language codes, not country codes. Did you mean '" + fix + "'?");
            }
            if (!LANG_CODE.matcher(lang).matches()) {
                throw CliException.invalidArgs("Invalid language code '" + lang + "'.",
                        "Use Wikipedia language codes like uk, pl, cs, de.");
            }
            result.add(lang);
        }
        if (result.isEmpty()) {
            throw CliException.invalidArgs("--langs is empty.", "Example: --langs pl,cs.");
        }
        if (result.size() > MAX_LANGS) {
            throw CliException.invalidArgs("Too many languages (" + result.size() + ").",
                    "Use at most " + MAX_LANGS + " languages per call.");
        }
        return List.copyOf(result);
    }

    private static int parseMonths(String value) {
        if (value == null) {
            return DEFAULT_MONTHS;
        }
        int months;
        try {
            months = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw CliException.invalidArgs("--months must be a number, got '" + value + "'.",
                    "Example: --months 36 for three years.");
        }
        if (months < MIN_MONTHS || months > MAX_MONTHS) {
            throw CliException.invalidArgs("--months must be " + MIN_MONTHS + "-" + MAX_MONTHS + ", got " + months + ".",
                    "Use --months 24 for two years, 36 for three.");
        }
        return months;
    }

    private YearMonth parseEnd(String value) {
        YearMonth lastComplete = YearMonth.now(clock).minusMonths(1);
        if (value == null) {
            return lastComplete;
        }
        YearMonth end;
        try {
            end = YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw CliException.invalidArgs("--end must be YYYY-MM, got '" + value + "'.", "Example: --end 2026-07.");
        }
        if (end.isAfter(lastComplete)) {
            throw CliException.invalidArgs("--end " + end + " is not a complete month yet.",
                    "Use --end " + lastComplete + " or earlier, or leave --end out.");
        }
        return end;
    }

    private static Map<String, String> parseManualTitles(List<String> raw, List<String> topics, List<String> langs) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        if (topics.size() > 1) {
            throw CliException.invalidArgs("--article works only with a single --topic.",
                    "Run a separate command for the topic that needs a manual title.");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String value : raw) {
            int colon = value.indexOf(':');
            if (colon <= 0 || colon == value.length() - 1) {
                throw CliException.invalidArgs("--article must be lang:Title, got '" + value + "'.",
                        "Example: --article cs:Šachy.");
            }
            String lang = value.substring(0, colon).strip().toLowerCase();
            String title = value.substring(colon + 1).strip();
            if (!langs.contains(lang)) {
                throw CliException.invalidArgs("--article language '" + lang + "' is not in --langs.",
                        "Add '" + lang + "' to --langs, or fix the --article prefix.");
            }
            if (result.putIfAbsent(lang, title) != null) {
                throw CliException.invalidArgs("--article for '" + lang + "' is given twice.",
                        "Give one --article per language.");
            }
        }
        return Map.copyOf(result);
    }
}
