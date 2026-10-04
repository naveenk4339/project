package com.pos.common.events;

/** Thrown for events that can never be processed; the Kafka error handler sends these straight to the DLT. */
public class MalformedEventException extends RuntimeException {

    public MalformedEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
