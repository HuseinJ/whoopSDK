package com.whoopsdk.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Query parameters shared by every collection endpoint.
 *
 * <p>{@code start} is inclusive, {@code end} is exclusive. Omitting {@code start} returns records
 * from the beginning of the user's history; omitting {@code end} returns records up to now.
 *
 * <pre>{@code
 * PageRequest.of(25).since(Instant.now().minus(7, ChronoUnit.DAYS));
 * PageRequest.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
 * }</pre>
 */
public final class PageRequest {

    /** The largest page size the WHOOP API accepts. */
    public static final int MAX_LIMIT = 25;

    private Integer limit;
    private Instant start;
    private Instant end;
    private String nextToken;

    private PageRequest() {
    }

    /** An unconstrained request: the API's default page size, the user's whole history. */
    public static PageRequest unpaged() {
        return new PageRequest();
    }

    /** A request with an explicit page size, capped at {@link #MAX_LIMIT}. */
    public static PageRequest of(int limit) {
        return new PageRequest().limit(limit);
    }

    /** Records from {@code start} (inclusive) up to now. */
    public static PageRequest since(Instant start) {
        return new PageRequest().start(start);
    }

    /** Records in {@code [start, end)}. */
    public static PageRequest between(Instant start, Instant end) {
        return new PageRequest().start(start).end(end);
    }

    /** Records in {@code [start, end)}, interpreting the dates as UTC midnights. */
    public static PageRequest between(LocalDate start, LocalDate end) {
        return between(start.atStartOfDay(ZoneOffset.UTC).toInstant(),
                end.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    /**
     * Sets the page size.
     *
     * @throws IllegalArgumentException if outside 1..{@value #MAX_LIMIT}
     */
    public PageRequest limit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        this.limit = limit;
        return this;
    }

    public PageRequest start(Instant start) {
        this.start = start;
        return this;
    }

    public PageRequest end(Instant end) {
        this.end = end;
        return this;
    }

    /** Token from a previous {@link Page#nextToken()}; set for you by the streaming helpers. */
    public PageRequest nextToken(String nextToken) {
        this.nextToken = nextToken;
        return this;
    }

    public Integer limitValue() {
        return limit;
    }

    public Instant startValue() {
        return start;
    }

    public Instant endValue() {
        return end;
    }

    public String nextTokenValue() {
        return nextToken;
    }

    /** A copy with a different continuation token, leaving this instance untouched. */
    public PageRequest withNextToken(String token) {
        PageRequest copy = new PageRequest();
        copy.limit = this.limit;
        copy.start = this.start;
        copy.end = this.end;
        copy.nextToken = token;
        return copy;
    }

    /** Renders the request as API query parameters; null values are dropped by the HTTP layer. */
    public Map<String, String> toQueryParams() {
        Map<String, String> params = new LinkedHashMap<>();
        if (limit != null) {
            params.put("limit", String.valueOf(limit));
        }
        if (start != null) {
            params.put("start", DateTimeFormatter.ISO_INSTANT.format(start));
        }
        if (end != null) {
            params.put("end", DateTimeFormatter.ISO_INSTANT.format(end));
        }
        if (nextToken != null && !nextToken.isBlank()) {
            params.put("nextToken", nextToken);
        }
        return params;
    }

    @Override
    public String toString() {
        return "PageRequest" + toQueryParams();
    }
}
