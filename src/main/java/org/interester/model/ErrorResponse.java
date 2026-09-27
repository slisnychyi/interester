package org.interester.model;

public record ErrorResponse(String status, ErrorInfo error) {

    public static ErrorResponse of(String code, String message, String hint) {
        return new ErrorResponse("error", new ErrorInfo(code, message, hint));
    }
}
