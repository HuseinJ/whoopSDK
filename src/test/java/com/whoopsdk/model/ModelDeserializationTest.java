package com.whoopsdk.model;

import com.fasterxml.jackson.databind.JavaType;
import com.whoopsdk.http.Json;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confirms the snake_case payloads from the WHOOP API map onto the record models. */
class ModelDeserializationTest {

    @Test
    void deserializesCycle() {
        String json = """
                {
                  "id": 93845,
                  "user_id": 10129,
                  "created_at": "2026-06-14T12:23:32.805Z",
                  "updated_at": "2026-06-14T12:23:32.805Z",
                  "start": "2026-06-13T02:58:17.049Z",
                  "end": "2026-06-14T02:58:17.049Z",
                  "timezone_offset": "+02:00",
                  "score_state": "SCORED",
                  "score": {
                    "strain": 5.2951527,
                    "kilojoule": 8288.297,
                    "average_heart_rate": 68,
                    "max_heart_rate": 141
                  },
                  "step_count": 8543
                }""";

        Cycle cycle = Json.read(json, Cycle.class);

        assertEquals(93845L, cycle.id());
        assertEquals(10129L, cycle.userId());
        assertEquals(Instant.parse("2026-06-13T02:58:17.049Z"), cycle.start());
        assertEquals("+02:00", cycle.timezoneOffset());
        assertEquals(ScoreState.SCORED, cycle.scoreState());
        assertEquals(8543, cycle.stepCount());
        assertFalse(cycle.isOngoing());
        assertEquals(68, cycle.score().averageHeartRate());
        assertEquals(1980.9, cycle.score().calories(), 1.0);
    }

    @Test
    void deserializesOngoingCycleWithoutScore() {
        String json = """
                {
                  "id": 1,
                  "user_id": 2,
                  "created_at": "2026-06-14T12:23:32.805Z",
                  "updated_at": "2026-06-14T12:23:32.805Z",
                  "start": "2026-06-14T02:58:17.049Z",
                  "end": null,
                  "timezone_offset": "+02:00",
                  "score_state": "PENDING_SCORE"
                }""";

        Cycle cycle = Json.read(json, Cycle.class);

        assertTrue(cycle.isOngoing());
        assertEquals(ScoreState.PENDING_SCORE, cycle.scoreState());
        assertFalse(cycle.scoreState().isScored());
        assertTrue(cycle.scoreOptional().isEmpty());
        assertNull(cycle.stepCount());
    }

    @Test
    void mapsUnknownScoreStateToUnknownInsteadOfFailing() {
        Cycle cycle = Json.read("""
                {"id":1,"user_id":2,"start":"2026-06-14T02:58:17.049Z","score_state":"SOMETHING_NEW"}""",
                Cycle.class);

        assertEquals(ScoreState.UNKNOWN, cycle.scoreState());
    }

    @Test
    void deserializesSleepWithStages() {
        String json = """
                {
                  "id": "ecfc6a15-4661-442f-a9a4-f160dd7afae8",
                  "v1_id": 93845,
                  "cycle_id": 93845,
                  "user_id": 10129,
                  "created_at": "2026-06-14T12:23:32.805Z",
                  "updated_at": "2026-06-14T12:23:32.805Z",
                  "start": "2026-06-13T22:30:00.000Z",
                  "end": "2026-06-14T06:30:00.000Z",
                  "timezone_offset": "+02:00",
                  "nap": false,
                  "score_state": "SCORED",
                  "score": {
                    "stage_summary": {
                      "total_in_bed_time_milli": 28800000,
                      "total_awake_time_milli": 1800000,
                      "total_no_data_time_milli": 0,
                      "total_light_sleep_time_milli": 14400000,
                      "total_slow_wave_sleep_time_milli": 7200000,
                      "total_rem_sleep_time_milli": 5400000,
                      "sleep_cycle_count": 4,
                      "disturbance_count": 7
                    },
                    "sleep_needed": {
                      "baseline_milli": 27000000,
                      "need_from_sleep_debt_milli": 1800000,
                      "need_from_recent_strain_milli": 900000,
                      "need_from_recent_nap_milli": -600000
                    },
                    "respiratory_rate": 16.1,
                    "sleep_performance_percentage": 98.0,
                    "sleep_consistency_percentage": 90.0,
                    "sleep_efficiency_percentage": 93.7
                  }
                }""";

        Sleep sleep = Json.read(json, Sleep.class);

        assertEquals("ecfc6a15-4661-442f-a9a4-f160dd7afae8", sleep.id().toString());
        assertEquals(93845L, sleep.v1Id());
        assertFalse(sleep.nap());
        assertEquals(8 * 60, sleep.timeInBed().toMinutes());

        SleepStageSummary stages = sleep.score().stageSummary();
        // 480 min in bed, 30 of them awake → 450 min across the three sleep stages.
        assertEquals(450, stages.totalAsleep().toMinutes());
        assertEquals(30, stages.awake().toMinutes());
        assertEquals(7, stages.disturbanceCount());

        // 450 baseline + 30 debt + 15 strain, less 10 minutes already slept in a nap.
        assertEquals(485, sleep.score().sleepNeeded().total().toMinutes());
        assertEquals(98.0f, sleep.score().performance().orElseThrow());
    }

    @Test
    void deserializesRecovery() {
        String json = """
                {
                  "cycle_id": 93845,
                  "sleep_id": "ecfc6a15-4661-442f-a9a4-f160dd7afae8",
                  "user_id": 10129,
                  "created_at": "2026-06-14T12:23:32.805Z",
                  "updated_at": "2026-06-14T12:23:32.805Z",
                  "score_state": "SCORED",
                  "score": {
                    "user_calibrating": false,
                    "recovery_score": 44.0,
                    "resting_heart_rate": 64.0,
                    "hrv_rmssd_milli": 31.813562,
                    "spo2_percentage": 95.6875,
                    "skin_temp_celsius": 33.7
                  }
                }""";

        Recovery recovery = Json.read(json, Recovery.class);

        assertEquals(93845L, recovery.cycleId());
        assertEquals("ecfc6a15-4661-442f-a9a4-f160dd7afae8", recovery.sleepId().toString());
        assertFalse(recovery.score().userCalibrating());
        assertEquals(44.0f, recovery.score().recoveryScore());
        assertEquals(31.813562f, recovery.score().hrvRmssdMilli());
        assertEquals(95.6875f, recovery.score().spo2().orElseThrow());
    }

    @Test
    void deserializesWorkoutWithZones() {
        String json = """
                {
                  "id": "ecfc6a15-4661-442f-a9a4-f160dd7afae8",
                  "user_id": 10129,
                  "created_at": "2026-06-14T12:23:32.805Z",
                  "updated_at": "2026-06-14T12:23:32.805Z",
                  "start": "2026-06-14T08:00:00.000Z",
                  "end": "2026-06-14T09:00:00.000Z",
                  "timezone_offset": "+02:00",
                  "sport_name": "running",
                  "score_state": "SCORED",
                  "score": {
                    "strain": 8.2,
                    "average_heart_rate": 123,
                    "max_heart_rate": 146,
                    "kilojoule": 1569.34,
                    "percent_recorded": 100.0,
                    "distance_meter": 1772.77,
                    "altitude_gain_meter": 46.64,
                    "altitude_change_meter": -0.38,
                    "zone_durations": {
                      "zone_zero_milli": 13458,
                      "zone_one_milli": 389000,
                      "zone_two_milli": 388000,
                      "zone_three_milli": 61000,
                      "zone_four_milli": 0,
                      "zone_five_milli": 0
                    }
                  }
                }""";

        Workout workout = Json.read(json, Workout.class);

        assertEquals("running", workout.sportName());
        assertEquals(60, workout.duration().toMinutes());
        assertEquals(1772.77f, workout.score().distance().orElseThrow());
        assertEquals(61, workout.score().zoneDurations().highIntensity().toSeconds());
        assertEquals(6, workout.score().zoneDurations().asList().size());
    }

    @Test
    void deserializesPaginatedResponse() {
        String json = """
                {
                  "records": [
                    {"id": 1, "user_id": 2, "start": "2026-06-13T02:58:17.049Z", "score_state": "SCORED"},
                    {"id": 2, "user_id": 2, "start": "2026-06-12T02:58:17.049Z", "score_state": "SCORED"}
                  ],
                  "next_token": "MTIzOjEyMz"
                }""";

        JavaType type = Json.mapper().getTypeFactory().constructParametricType(Page.class, Cycle.class);
        Page<Cycle> page = Json.read(json, type);

        assertEquals(2, page.size());
        assertTrue(page.hasNext());
        assertEquals("MTIzOjEyMz", page.nextToken());
        assertEquals(1L, page.records().get(0).id());
    }

    @Test
    void lastPageHasNoNextToken() {
        JavaType type = Json.mapper().getTypeFactory().constructParametricType(Page.class, Cycle.class);
        Page<Cycle> page = Json.read("{\"records\": []}", type);

        assertFalse(page.hasNext());
        assertTrue(page.isEmpty());
        assertTrue(page.nextTokenOptional().isEmpty());
    }

    @Test
    void deserializesUserRecords() {
        UserBasicProfile profile = Json.read("""
                {"user_id": 10129, "email": "jsmith123@whoop.com", "first_name": "John", "last_name": "Smith"}""",
                UserBasicProfile.class);
        assertEquals("John Smith", profile.fullName());
        assertEquals(10129L, profile.userId());

        UserBodyMeasurement body = Json.read("""
                {"height_meter": 1.8288, "weight_kilogram": 90.7185, "max_heart_rate": 200}""",
                UserBodyMeasurement.class);
        assertEquals(1.8288f, body.heightMeter());
        assertEquals(200, body.maxHeartRate());
    }
}
