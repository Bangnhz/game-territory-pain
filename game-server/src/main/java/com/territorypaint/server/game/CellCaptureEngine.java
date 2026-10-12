package com.territorypaint.server.game;

import com.territorypaint.common.dto.PositionDTO;
import com.territorypaint.common.protocol.Direction;

/**
 * Authoritative cell capture and movement engine.
 * Requirements: 5.4, 5.5, 6.8, 6.9, 6.10, 6.11, 6.12, 6.13, 6.14
 */
public class CellCaptureEngine {

    public static class MoveResult {
        public final boolean valid;
        public final String errorCode;
        public final PositionDTO newPosition;

        public MoveResult(boolean valid, String errorCode, PositionDTO newPosition) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.newPosition = newPosition;
        }

        public static MoveResult success(PositionDTO pos) {
            return new MoveResult(true, null, pos);
        }

        public static MoveResult reject(String errorCode, PositionDTO currentPos) {
            return new MoveResult(false, errorCode, currentPos);
        }
    }

    /**
     * Process a movement request against the grid and update territory scores.
     * Requirements: 6.8-6.14
     */
    public MoveResult applyMove(int[][] grid, PositionDTO currentPos, Direction direction, int playerSlot, int[] territoryScores) {
        int newX = currentPos.getX() + direction.getDeltaX();
        int newY = currentPos.getY() + direction.getDeltaY();

        // Check grid boundary (Requirement 5.4, 6.9: 20x20 grid, 0..19)
        if (newX < 0 || newX >= 20 || newY < 0 || newY >= 20) {
            return MoveResult.reject("OUT_OF_BOUNDS", currentPos);
        }

        PositionDTO newPos = new PositionDTO(newX, newY);
        int targetCellOwner = grid[newY][newX]; // grid[y][x]

        if (targetCellOwner == playerSlot) {
            // Requirement 6.11: Already owned cell -> preserve ownership and scores
        } else if (targetCellOwner == 0) {
            // Requirement 6.10: Unowned cell -> assign ownership, score +1
            grid[newY][newX] = playerSlot;
            territoryScores[playerSlot - 1]++;
        } else {
            // Requirement 6.12: Opponent cell -> transfer ownership, moving +1, opponent -1
            grid[newY][newX] = playerSlot;
            territoryScores[playerSlot - 1]++;
            int opponentSlot = (playerSlot == 1) ? 2 : 1;
            territoryScores[opponentSlot - 1]--;
        }

        return MoveResult.success(newPos);
    }
}
