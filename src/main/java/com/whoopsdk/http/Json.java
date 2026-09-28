package com.whoopsdk.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.whoopsdk.exception.WhoopException;

/**
 * Central Jackson configuration.
 *
 * <p>The WHOOP API uses {@code snake_case} everywhere, so the shared mapper applies that naming
 * strategy and every model in this SDK can use idiomatic camelCase component names.
 */
public final class Json {

    private static final ObjectMapper MAPPER = newMapper();

    private Json() {
    }

    /** A mapper configured exactly like the one the SDK uses internally. */
    public static ObjectMapper newMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new WhoopException("Failed to parse WHOOP response as " + type.getSimpleName(), e);
        }
    }

    public static <T> T read(String json, JavaType type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new WhoopException("Failed to parse WHOOP response as " + type, e);
        }
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new WhoopException("Failed to serialize " + value, e);
        }
    }
}
