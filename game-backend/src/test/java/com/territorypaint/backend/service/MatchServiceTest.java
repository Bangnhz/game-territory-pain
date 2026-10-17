package com.territorypaint.backend.service;

import com.territorypaint.backend.dto.*;
import com.territorypaint.backend.entity.*;
import com.territorypaint.backend.exception.MatchResultConflictException;
import com.territorypaint.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchHistoryRepository matchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @InjectMocks
    private MatchService matchService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("player1", "hash1", "Player One");
        user1.setId(1L);
        user1.setTotalScore(100L);

        user2 = new User("player2", "hash2", "Player Two");
        user2.setId(2L);
        user2.setTotalScore(100L);
    }

    @Test
    void testSaveMatch_NewMatchSuccess() {
        SaveMatchRequest request = createRequest("match-1", "room-1", "TIMEOUT", null, 200, 150);

        when(matchRepository.findById("match-1")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        UserStats stats1 = new UserStats(user1);
        UserStats stats2 = new UserStats(user2);
        when(userStatsRepository.findByUserIdInForUpdate(anyList())).thenReturn(Arrays.asList(stats1, stats2));

        SaveMatchResponse response = matchService.saveMatch(request);

        assertTrue(response.isSuccess());
        assertFalse(response.isAlreadyExists());
        assertEquals("match-1", response.getMatchId());

        assertEquals(110L, user1.getTotalScore());
        assertEquals(95L, user2.getTotalScore());

        verify(matchRepository).save(any(Match.class));
        verify(matchHistoryRepository, times(2)).save(any(MatchHistory.class));
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void testSaveMatch_LossClampedToZeroWhenScoreIsLow() {
        user2.setTotalScore(3L);
        SaveMatchRequest request = createRequest("match-2", "room-1", "TIMEOUT", null, 200, 150);

        when(matchRepository.findById("match-2")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        UserStats stats1 = new UserStats(user1);
        UserStats stats2 = new UserStats(user2);
        when(userStatsRepository.findByUserIdInForUpdate(anyList())).thenReturn(Arrays.asList(stats1, stats2));

        SaveMatchResponse response = matchService.saveMatch(request);

        assertTrue(response.isSuccess());
        // Player 2 had 3 LP, lost 5 -> clamped to 0 LP (never negative)
        assertEquals(0L, user2.getTotalScore());
    }

    @Test
    void testSaveMatch_IdempotentIdenticalPayload() {
        SaveMatchRequest request = createRequest("match-1", "room-1", "TIMEOUT", null, 200, 150);

        Match existingMatch = new Match("match-1", "room-1", Instant.now(), Instant.now(), "TIMEOUT", 1L, null,
                calculateHash(request));
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(existingMatch));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userStatsRepository.findById(1L)).thenReturn(Optional.of(new UserStats(user1)));
        when(userStatsRepository.findById(2L)).thenReturn(Optional.of(new UserStats(user2)));

        SaveMatchResponse response = matchService.saveMatch(request);

        assertTrue(response.isSuccess());
        assertTrue(response.isAlreadyExists());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void testSaveMatch_IdempotentConflictDifferentPayload() {
        SaveMatchRequest request = createRequest("match-1", "room-1", "TIMEOUT", null, 200, 150);

        Match existingMatch = new Match("match-1", "room-1", Instant.now(), Instant.now(), "TIMEOUT", 1L, null,
                "different-hash");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(existingMatch));

        assertThrows(MatchResultConflictException.class, () -> matchService.saveMatch(request));
    }

    @Test
    void testGetMatchHistory_Success() {
        MatchHistory mh = new MatchHistory("match-1", 1L, 2L, 1, 200, "WIN", 10);
        Page<MatchHistory> page = new PageImpl<>(List.of(mh));

        when(matchHistoryRepository.findHistoryByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(matchRepository.findById("match-1")).thenReturn(
                Optional.of(new Match("match-1", "room-1", Instant.now(), Instant.now(), "TIMEOUT", 1L, null, "hash")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(matchHistoryRepository.findByMatchIdAndUserIdNot("match-1", 1L))
                .thenReturn(Optional.of(new MatchHistory("match-1", 2L, 1L, 2, 150, "LOSS", 0)));

        PageResponse<MatchHistoryItemDTO> result = matchService.getMatchHistory(1L, 0, 20);

        assertEquals(1, result.getItems().size());
        MatchHistoryItemDTO item = result.getItems().get(0);
        assertEquals("match-1", item.getMatchId());
        assertEquals("player2", item.getOpponentUsername());
        assertEquals(200, item.getMyCells());
        assertEquals(150, item.getOpponentCells());
        assertEquals("WIN", item.getOutcome());
    }

    private SaveMatchRequest createRequest(String matchId, String roomId, String endReason, Long forfeitedUserId,
            int cells1, int cells2) {
        Instant now = Instant.now();
        List<PlayerEntry> players = Arrays.asList(
                new PlayerEntry(1L, 1, cells1),
                new PlayerEntry(2L, 2, cells2));
        return new SaveMatchRequest(matchId, roomId, now, now.plusSeconds(60), endReason, players, forfeitedUserId);
    }

    private String calculateHash(SaveMatchRequest request) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(request.getMatchId()).append(";")
                    .append(request.getRoomId()).append(";")
                    .append(request.getEndReason()).append(";")
                    .append(request.getForfeitedUserId()).append(";");
            List<PlayerEntry> sorted = new ArrayList<>(request.getPlayers());
            sorted.sort(Comparator.comparing(PlayerEntry::getPlayerSlot));
            for (PlayerEntry p : sorted) {
                sb.append(p.getUserId()).append(":").append(p.getPlayerSlot()).append(":").append(p.getCellsOwned())
                        .append(";");
            }
            byte[] hashBytes = digest.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1)
                    hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
