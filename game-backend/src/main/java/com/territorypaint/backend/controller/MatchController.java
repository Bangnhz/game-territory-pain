package com.territorypaint.backend.controller;

import com.territorypaint.backend.dto.*;
import com.territorypaint.backend.exception.ErrorResponse;
import com.territorypaint.backend.exception.MatchResultConflictException;
import com.territorypaint.backend.security.JwtUser;
import com.territorypaint.backend.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for match operations (save results and fetch match history).
 * Requirements: 9.4, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 19.4, 19.5, 19.6, 19.12
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /**
     * Save match result.
     * Requirements: 9.4, 9.5-9.16, 19.5, 19.6, 19.12
     * Authenticated using X-Service-Key (ServiceKeyFilter).
     */
    @PostMapping("/save")
    public ResponseEntity<SaveMatchResponse> saveMatch(@Valid @RequestBody SaveMatchRequest request) {
        SaveMatchResponse response = matchService.saveMatch(request);
        if (response.isAlreadyExists()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    }

    /**
     * Get match history for the authenticated user.
     * Requirements: 11.1-11.9, 19.3
     * Authenticated using JWT (Authorization: Bearer <token>).
     */
    @GetMapping("/history")
    public ResponseEntity<PageResponse<MatchHistoryItemDTO>> getHistory(
            @AuthenticationPrincipal JwtUser jwtUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResponse<MatchHistoryItemDTO> response = matchService.getMatchHistory(jwtUser.getUserId(), page, size);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(MatchResultConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(MatchResultConflictException ex) {
        ErrorResponse error = new ErrorResponse("MATCH_RESULT_CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessable(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_MATCH_RESULT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
}
