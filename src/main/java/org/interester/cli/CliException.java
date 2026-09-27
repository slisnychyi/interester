package org.interester.cli;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class CliException extends RuntimeException {

    public static final int EXIT_INTERNAL = 1;
    public static final int EXIT_INVALID_ARGS = 2;
    public static final int EXIT_API_ERROR = 3;

    private final String code;
    private final String hint;
    private final int exitCode;

    private CliException(String code, String message, String hint, int exitCode) {
        super(message);
        this.code = code;
        this.hint = hint;
        this.exitCode = exitCode;
    }

    public static CliException invalidArgs(String message, String hint) {
        return new CliException("INVALID_ARGS", message, hint, EXIT_INVALID_ARGS);
    }

    public static CliException topicNotFound(String enTitle) {
        return new CliException("TOPIC_NOT_FOUND", "No English Wikipedia article titled '" + enTitle + "'.",
                "Use the exact title of an English Wikipedia article, e.g. 'Intermittent fasting'. "
                        + "Check spelling, or try a more general topic.",
                EXIT_INVALID_ARGS);
    }

    public static CliException apiError(String message) {
        return new CliException("API_ERROR", message,
                "Wikipedia may be down or rate-limiting. Wait a minute, then run the same command again.",
                EXIT_API_ERROR);
    }
}
