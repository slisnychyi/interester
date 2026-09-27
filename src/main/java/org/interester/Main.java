package org.interester;

import lombok.experimental.UtilityClass;
import org.interester.analyze.Analyzer;
import org.interester.analyze.Analyzer.Analysis;
import org.interester.cli.ArgsParser;
import org.interester.cli.CliArgs;
import org.interester.cli.CliException;
import org.interester.cli.Command;
import org.interester.data.WikiData;
import org.interester.model.AnalyzeResponse;
import org.interester.model.ErrorResponse;
import org.interester.report.Recommendation;
import org.interester.report.ReportWriter;

import java.io.PrintStream;
import java.time.Clock;
import java.time.LocalDate;

@UtilityClass
public class Main {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Clock clock = Clock.systemUTC();
        try {
            CliArgs cli = new ArgsParser(clock).parse(args);
            Analysis analysis = new Analyzer(WikiData.create()).analyze(cli);
            AnalyzeResponse response = analysis.response().withSummary(Recommendation.summary(analysis.response()));
            if (cli.command() == Command.REPORT) {
                response = response.withFiles(new ReportWriter(LocalDate.now(clock)).write(analysis, cli.out()));
            }
            JsonOutput.write(response, out);
            return 0;
        } catch (CliException e) {
            JsonOutput.write(ErrorResponse.of(e.code(), e.getMessage(), e.hint()), out);
            return e.exitCode();
        } catch (RuntimeException e) {
            e.printStackTrace(err);
            JsonOutput.write(ErrorResponse.of("INTERNAL_ERROR", String.valueOf(e.getMessage()),
                    "This is a bug in the skill. Tell the user; do not retry."), out);
            return CliException.EXIT_INTERNAL;
        }
    }
}
