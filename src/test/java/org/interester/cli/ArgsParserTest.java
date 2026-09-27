package org.interester.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgsParserTest {

    private final ArgsParser parser = new ArgsParser(
            Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC));

    private CliArgs parse(String... args) {
        return parser.parse(args);
    }

    private CliException fails(String... args) {
        CliException e = assertThrows(CliException.class, () -> parser.parse(args));
        assertEquals(CliException.EXIT_INVALID_ARGS, e.exitCode());
        assertTrue(e.hint() != null && !e.hint().isBlank(), "every error needs a hint");
        return e;
    }

    @Test
    void defaults() {
        CliArgs a = parse("analyze", "--topic", "Astronomy", "--langs", "uk");
        assertEquals(Command.ANALYZE, a.command());
        assertEquals(List.of("Astronomy"), a.topics());
        assertEquals(List.of("uk"), a.langs());
        assertEquals(24, a.months());
        assertEquals(YearMonth.of(2026, 8), a.end());
        assertEquals(YearMonth.of(2024, 9), a.from());
        assertEquals(Map.of(), a.manualTitles());
        assertEquals(Path.of("out"), a.out());
    }

    @Test
    void allOptions() {
        CliArgs a = parse("report", "--topic", "Intermittent fasting", "--topic", "Washington, D.C.",
                "--langs", "PL, cs,pl", "--months", "36", "--end", "2026-07", "--out", "reports");
        assertEquals(List.of("Intermittent fasting", "Washington, D.C."), a.topics());
        assertEquals(List.of("pl", "cs"), a.langs());
        assertEquals(36, a.months());
        assertEquals(YearMonth.of(2023, 8), a.from());
        assertEquals(Path.of("reports"), a.out());

        assertEquals(Map.of("cs", "Šachy"), parse("analyze", "--topic", "Chess",
                "--langs", "pl,cs", "--article", "cs:Šachy").manualTitles());
    }

    @Test
    void badArgsFailWithHint() {
        String[][] bad = {
                {},
                {"compare", "--topic", "X", "--langs", "uk"},
                {"analyze", "--langs", "uk"},
                {"analyze", "--topic", "X"},
                {"analyze", "--topic", "--langs", "uk"},
                {"analyze", "--topic", "X", "--langs", "uk", "--period", "2y"},
                {"analyze", "--topic", "X", "--langs", "Ukrainian"},
                {"analyze", "--topic", "X", "--langs", ","},
                {"analyze", "--topic", "X", "--langs", "de,fr,it,es,pt,pl,cs,sk,uk,tr,nl"},
                {"analyze", "--topic", "X", "--langs", "uk", "--langs", "pl"},
                {"analyze", "--topic", "X", "--langs", "uk", "--months", "two"},
                {"analyze", "--topic", "X", "--langs", "uk", "--months", "6"},
                {"analyze", "--topic", "X", "--langs", "uk", "--end", "2026-09"},
                {"analyze", "--topic", "X", "--langs", "uk", "--end", "07-2026"},
                {"analyze", "--topic", "X", "--langs", "uk", "--end", "2016-01", "--months", "24"},
                {"analyze", "--topic", "X", "--langs", "pl", "--article", "Šachy"},
                {"analyze", "--topic", "X", "--langs", "pl", "--article", "cs:Něco"},
                {"analyze", "--topic", "X", "--topic", "Y", "--langs", "pl", "--article", "pl:Z"},
                {"analyze", "--topic", "X", "--langs", "uk", "--out", "dir"},
        };
        for (String[] args : bad) {
            fails(args);
        }
    }

    @Test
    void commonMistakesGetASpecificHint() {
        assertTrue(fails("analyze", "--topic", "Intermittent", "fasting", "--langs", "pl").hint().contains("quotes"));
        assertTrue(fails("analyze", "--topic", "Astronomy", "--langs", "ua").hint().contains("'uk'"));
        assertTrue(fails("report", "--topic", "A", "--topic", "B", "--langs", "uk,pl,cs,sk,de,fr,es")
                .getMessage().contains("(14)"));
    }
}
