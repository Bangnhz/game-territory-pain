package com.territorypaint.backend.validation;

import com.territorypaint.backend.dto.PlayerEntry;
import com.territorypaint.backend.dto.SaveMatchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for SaveMatchRequest complex constraints.
 * Requirements: 9.6, 9.7
 */
public class MatchRequestValidator implements ConstraintValidator<ValidMatchRequest, SaveMatchRequest> {
    
    @Override
    public void initialize(ValidMatchRequest constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(SaveMatchRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // @NotNull handles null check
        }
        
        context.disableDefaultConstraintViolation();
        boolean isValid = true;
        
        List<PlayerEntry> players = request.getPlayers();
        
        // Validate exactly two players (handled by @Size, but we check here for safety)
        if (players == null || players.size() != 2) {
            context.buildConstraintViolationWithTemplate("Exactly two players are required")
                   .addConstraintViolation();
            return false;
        }
        
        // Validate unique player IDs
        Set<Long> userIds = new HashSet<>();
        for (PlayerEntry player : players) {
            if (player.getUserId() != null) {
                if (!userIds.add(player.getUserId())) {
                    context.buildConstraintViolationWithTemplate("Player IDs must be unique")
                           .addPropertyNode("players")
                           .addConstraintViolation();
                    isValid = false;
                }
            }
        }
        
        // Validate valid player slots (1 and 2)
        Set<Integer> slots = new HashSet<>();
        for (PlayerEntry player : players) {
            if (player.getPlayerSlot() != null) {
                slots.add(player.getPlayerSlot());
            }
        }
        if (!slots.contains(1) || !slots.contains(2)) {
            context.buildConstraintViolationWithTemplate("Player slots must be 1 and 2")
                   .addPropertyNode("players")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Validate cell counts sum <= 400
        int totalCells = 0;
        for (PlayerEntry player : players) {
            if (player.getCellsOwned() != null) {
                totalCells += player.getCellsOwned();
            }
        }
        if (totalCells > 400) {
            context.buildConstraintViolationWithTemplate("Sum of cells owned cannot exceed 400")
                   .addPropertyNode("players")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Validate timestamps are consistent
        if (request.getStartedAt() != null && request.getEndedAt() != null) {
            if (request.getEndedAt().isBefore(request.getStartedAt())) {
                context.buildConstraintViolationWithTemplate("End time must be after start time")
                       .addPropertyNode("endedAt")
                       .addConstraintViolation();
                isValid = false;
            }
        }
        
        // Validate forfeitedUserId consistency with endReason
        if ("FORFEIT".equals(request.getEndReason())) {
            if (request.getForfeitedUserId() == null) {
                context.buildConstraintViolationWithTemplate("Forfeited user ID is required when end reason is FORFEIT")
                       .addPropertyNode("forfeitedUserId")
                       .addConstraintViolation();
                isValid = false;
            } else {
                // Verify forfeitedUserId is one of the players
                boolean foundForfeiter = false;
                for (PlayerEntry player : players) {
                    if (player.getUserId() != null && player.getUserId().equals(request.getForfeitedUserId())) {
                        foundForfeiter = true;
                        break;
                    }
                }
                if (!foundForfeiter) {
                    context.buildConstraintViolationWithTemplate("Forfeited user ID must be one of the players")
                           .addPropertyNode("forfeitedUserId")
                           .addConstraintViolation();
                    isValid = false;
                }
            }
        } else if ("TIMEOUT".equals(request.getEndReason())) {
            if (request.getForfeitedUserId() != null) {
                context.buildConstraintViolationWithTemplate("Forfeited user ID must be null when end reason is TIMEOUT")
                       .addPropertyNode("forfeitedUserId")
                       .addConstraintViolation();
                isValid = false;
            }
        }
        
        return isValid;
    }
}
