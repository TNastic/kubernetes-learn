package com.example.taskmanager.common;

import java.time.Instant;

public class ErrorResponse {

    private final String message;
    private final Instant timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
