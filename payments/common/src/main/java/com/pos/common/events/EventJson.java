package com.pos.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Single place that defines how events are (de)serialized so every service agrees on the format. */
public final class EventJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private EventJson() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize " + value.getClass().getSimpleName(), e);
        }
    }

    public static EventEnvelope readEnvelope(String json) {
        try {
            return MAPPER.readValue(json, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new MalformedEventException("Not a valid event envelope", e);
        }
    }

    public static <T> T payload(EventEnvelope envelope, Class<T> type) {
        try {
            return MAPPER.treeToValue(envelope.payload(), type);
        } catch (JsonProcessingException e) {
            throw new MalformedEventException("Payload of " + envelope.eventId() + " is not a " + type.getSimpleName(), e);
        }
    }
}
