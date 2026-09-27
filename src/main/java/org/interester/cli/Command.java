package org.interester.cli;

public enum Command {
    ANALYZE,
    REPORT;

    public static Command parse(String name) {
        return switch (name) {
            case "analyze" -> ANALYZE;
            case "report" -> REPORT;
            default -> throw CliException.invalidArgs(
                    "Unknown command '" + name + "'.",
                    ArgsParser.USAGE);
        };
    }
}
