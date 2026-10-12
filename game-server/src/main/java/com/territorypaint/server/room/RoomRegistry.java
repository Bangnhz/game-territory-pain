package com.territorypaint.server.room;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking active GameRoom instances.
 * Requirements: 5.1, 14.3, 14.13
 */
public class RoomRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RoomRegistry.class);
    private static final int MAX_ROOMS = 50; // Requirement 14.13

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public synchronized boolean registerRoom(GameRoom room) {
        if (rooms.size() >= MAX_ROOMS) {
            logger.warn("Maximum room capacity reached (50 rooms)");
            return false;
        }
        rooms.put(room.getRoomId(), room);
        logger.info("Registered room {} (active count: {})", room.getRoomId(), rooms.size());
        return true;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public synchronized void removeRoom(String roomId) {
        if (rooms.remove(roomId) != null) {
            logger.info("Removed room {} (active count: {})", roomId, rooms.size());
        }
    }
}
