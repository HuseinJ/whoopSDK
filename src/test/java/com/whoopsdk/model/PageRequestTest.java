package com.whoopsdk.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageRequestTest {

    @Test
    void rendersInstantsAsIso8601() {
        Map<String, String> params = PageRequest
                .between(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"))
                .limit(10)
                .toQueryParams();

        assertEquals("10", params.get("limit"));
        assertEquals("2026-01-01T00:00:00Z", params.get("start"));
        assertEquals("2026-02-01T00:00:00Z", params.get("end"));
        assertFalse(params.containsKey("nextToken"));
    }

    @Test
    void treatsLocalDatesAsUtcMidnight() {
        Map<String, String> params = PageRequest
                .between(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8))
                .toQueryParams();

        assertEquals("2026-03-01T00:00:00Z", params.get("start"));
        assertEquals("2026-03-08T00:00:00Z", params.get("end"));
    }

    @Test
    void omitsUnsetParameters() {
        assertTrue(PageRequest.unpaged().toQueryParams().isEmpty());
    }

    @Test
    void rejectsLimitAboveApiMaximum() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(26));
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0));
    }

    @Test
    void withNextTokenCopiesRatherThanMutates() {
        PageRequest original = PageRequest.of(5).start(Instant.parse("2026-01-01T00:00:00Z"));
        PageRequest next = original.withNextToken("abc");

        assertFalse(original.toQueryParams().containsKey("nextToken"));
        assertEquals("abc", next.toQueryParams().get("nextToken"));
        assertEquals("5", next.toQueryParams().get("limit"));
        assertEquals("2026-01-01T00:00:00Z", next.toQueryParams().get("start"));
    }
}
