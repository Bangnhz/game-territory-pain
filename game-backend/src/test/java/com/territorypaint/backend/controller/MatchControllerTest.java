package com.territorypaint.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.territorypaint.backend.dto.*;
import com.territorypaint.backend.exception.MatchResultConflictException;
import com.territorypaint.backend.security.JwtTokenProvider;
import com.territorypaint.backend.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchService matchService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testSaveMatch_Created() throws Exception {
        SaveMatchRequest request = createValidRequest();
        SaveMatchResponse response = SaveMatchResponse.newlySaved(request.getMatchId(), List.of());

        when(matchService.saveMatch(any(SaveMatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(request.getMatchId()))
                .andExpect(jsonPath("$.alreadyExists").value(false));
    }

    @Test
    void testSaveMatch_AlreadySavedOk() throws Exception {
        SaveMatchRequest request = createValidRequest();
        SaveMatchResponse response = SaveMatchResponse.alreadySaved(request.getMatchId(), List.of());

        when(matchService.saveMatch(any(SaveMatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(request.getMatchId()))
                .andExpect(jsonPath("$.alreadyExists").value(true));
    }

    @Test
    void testSaveMatch_Conflict() throws Exception {
        SaveMatchRequest request = createValidRequest();

        when(matchService.saveMatch(any(SaveMatchRequest.class)))
                .thenThrow(new MatchResultConflictException("Match result conflict"));

        mockMvc.perform(post("/api/matches/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MATCH_RESULT_CONFLICT"));
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
