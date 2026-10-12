package com.territorypaint.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SaveMatchRequest validation.
 * Requirements: 9.5, 9.6, 9.7
 */
class SaveMatchRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRequest() {
        SaveMatchRequest request = createValidRequest();
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testMissingMatchId() {
        SaveMatchRequest request = createValidRequest();
        request.setMatchId(null);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Match ID is required")));
    }

    @Test
    void testInvalidMatchIdFormat() {
        SaveMatchRequest request = createValidRequest();
        request.setMatchId("invalid-uuid");
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("valid UUID")));
    }

    @Test
    void testMissingRoomId() {
        SaveMatchRequest request = createValidRequest();
        request.setRoomId(null);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Room ID is required")));
    }

    @Test
    void testInvalidRoomIdFormat() {
        SaveMatchRequest request = createValidRequest();
        request.setRoomId("not-a-uuid");
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("valid UUID")));
    }

    @Test
    void testMissingStartTime() {
        SaveMatchRequest request = createValidRequest();
        request.setStartedAt(null);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Start time is required")));
    }

    @Test
    void testMissingEndTime() {
        SaveMatchRequest request = createValidRequest();
        request.setEndedAt(null);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("End time is required")));
    }

    @Test
    void testEndTimeBeforeStartTime() {
        SaveMatchRequest request = createValidRequest();
        Instant now = Instant.now();
        request.setStartedAt(now);
        request.setEndedAt(now.minusSeconds(60));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("after start time")));
    }

    @Test
    void testInvalidEndReason() {
        SaveMatchRequest request = createValidRequest();
        request.setEndReason("INVALID");
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("TIMEOUT or FORFEIT")));
    }

    @Test
    void testMissingPlayers() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(null);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Players list is required")));
    }

    @Test
    void testTooFewPlayers() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(new PlayerEntry(1L, 1, 200)));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Exactly two players")));
    }

    @Test
    void testTooManyPlayers() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
                new PlayerEntry(1L, 1, 100),
                new PlayerEntry(2L, 2, 100),
                new PlayerEntry(3L, 2, 100)));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Exactly two players")));
    }

    @Test
    void testDuplicatePlayerIds() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, 200),
            new PlayerEntry(1L, 2, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player IDs must be unique")));
    }

    @Test
    void testInvalidPlayerSlots() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 0, 200),
            new PlayerEntry(2L, 3, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("Player slot must be 1 or 2") || 
            v.getMessage().contains("Player slots must be 1 and 2")));
    }

    @Test
    void testDuplicatePlayerSlots() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, 200),
            new PlayerEntry(2L, 1, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player slots must be 1 and 2")));
    }

    @Test
    void testCellCountExceedsMaximum() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, 250),
            new PlayerEntry(2L, 2, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot exceed 400")));
    }

    @Test
    void testCellCountAtMaximum() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, 200),
            new PlayerEntry(2L, 2, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "400 total cells should be valid");
    }

    @Test
    void testNegativeCellCount() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, -10),
            new PlayerEntry(2L, 2, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("between 0 and 400")));
    }

    @Test
    void testCellCountAbove400() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, 500),
            new PlayerEntry(2L, 2, 200)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("between 0 and 400")));
    }

    @Test
    void testForfeitWithoutForfeitedUserId() {
        SaveMatchRequest request = createValidRequest();
        request.setEndReason("FORFEIT");
        request.setForfeitedUserId(null);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("Forfeited user ID is required when end reason is FORFEIT")));
    }

    @Test
    void testTimeoutWithForfeitedUserId() {
        SaveMatchRequest request = createValidRequest();
        request.setEndReason("TIMEOUT");
        request.setForfeitedUserId(1L);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("Forfeited user ID must be null when end reason is TIMEOUT")));
    }

    @Test
    void testForfeitedUserIdNotInPlayers() {
        SaveMatchRequest request = createValidRequest();
        request.setEndReason("FORFEIT");
        request.setForfeitedUserId(999L);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("Forfeited user ID must be one of the players")));
    }

    @Test
    void testValidForfeitRequest() {
        SaveMatchRequest request = createValidRequest();
        request.setEndReason("FORFEIT");
        request.setForfeitedUserId(1L);
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid forfeit request should have no violations");
    }

    @Test
    void testBoundaryValues() {
        SaveMatchRequest request = createValidRequest();
        request.setPlayers(Arrays.asList(
            new PlayerEntry(1L, 1, 0),
            new PlayerEntry(2L, 2, 400)
        ));
        Set<ConstraintViolation<SaveMatchRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Boundary values (0 and 400) should be valid");
    }

    private SaveMatchRequest createValidRequest() {
        Instant now = Instant.now();
        List<PlayerEntry> players = Arrays.asList(
            new PlayerEntry(1L, 1, 150),
            new PlayerEntry(2L, 2, 200)
        );
        return new SaveMatchRequest(
            "550e8400-e29b-41d4-a716-446655440000",
            "660e8400-e29b-41d4-a716-446655440000",
            now,
            now.plusSeconds(60),
            "TIMEOUT",
            players,
            null
        );
    }
}
