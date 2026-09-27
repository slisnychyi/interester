package org.interester.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Confidence {
    HIGH, MEDIUM, LOW;

    @JsonValue
    public String json() {
        return name().toLowerCase();
    }
}
