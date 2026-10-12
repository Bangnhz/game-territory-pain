package com.territorypaint.server.game;

import com.territorypaint.common.dto.PositionDTO;
import com.territorypaint.common.protocol.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CellCaptureEngineTest {

    private CellCaptureEngine engine;
    private int[][] grid;
    private int[] scores;

    @BeforeEach
    public void setUp() {
        engine = new CellCaptureEngine();
        grid = new int[20][20];
        scores = new int[]{1, 1}; // Initial spawn slots (0,0) for P1, (19,19) for P2
        grid[0][0] = 1;
        grid[19][19] = 2;
    }

    @Test
    public void testValidMoveToUnownedCell() {
        PositionDTO currentPos = new PositionDTO(0, 0);
        CellCaptureEngine.MoveResult result = engine.applyMove(grid, currentPos, Direction.DOWN, 1, scores);

        assertTrue(result.valid);
        assertEquals(0, result.newPosition.getX());
        assertEquals(1, result.newPosition.getY());
        assertEquals(1, grid[1][0]);
        assertEquals(2, scores[0]); // Player 1 score incremented from 1 to 2
        assertEquals(1, scores[1]);
    }

    @Test
    public void testOutOfBoundsMoveRejected() {
        PositionDTO currentPos = new PositionDTO(0, 0);
        CellCaptureEngine.MoveResult result = engine.applyMove(grid, currentPos, Direction.UP, 1, scores);

        assertFalse(result.valid);
        assertEquals("OUT_OF_BOUNDS", result.errorCode);
        assertEquals(0, result.newPosition.getX());
        assertEquals(0, result.newPosition.getY());
        assertEquals(1, scores[0]);
    }

    @Test
    public void testMoveToOpponentCell() {
        grid[0][1] = 2; // Opponent owns (1,0)
        scores[0] = 1;
        scores[1] = 2;

        PositionDTO currentPos = new PositionDTO(0, 0);
        CellCaptureEngine.MoveResult result = engine.applyMove(grid, currentPos, Direction.RIGHT, 1, scores);

        assertTrue(result.valid);
        assertEquals(1, grid[0][1]); // Ownership transferred to P1
        assertEquals(2, scores[0]); // P1 score +1
        assertEquals(1, scores[1]); // P2 score -1
    }

    @Test
    public void testMoveToSelfOwnedCellPreservesScore() {
        grid[0][1] = 1; // P1 owns (1,0)
        scores[0] = 2;

        PositionDTO currentPos = new PositionDTO(0, 0);
        CellCaptureEngine.MoveResult result = engine.applyMove(grid, currentPos, Direction.RIGHT, 1, scores);

        assertTrue(result.valid);
        assertEquals(1, grid[0][1]);
        assertEquals(2, scores[0]); // Score unchanged
    }
}
