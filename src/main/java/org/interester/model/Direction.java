package org.interester.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Direction {
    RISING, FALLING, FLAT;

    @JsonValue
    public String json() {
        return name().toLowerCase();
    }
}
