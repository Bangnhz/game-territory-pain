package com.territorypaint.server.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Local persistent file-based outbox for match results.
 * Requirements: 9.1, 9.2, 9.21, 9.22
 */
public class ResultOutbox {

    private static final Logger logger = LoggerFactory.getLogger(ResultOutbox.class);

    private final File storageDir;
    private final ObjectMapper objectMapper;

    public ResultOutbox(String storageDirPath) {
        this.storageDir = new File(storageDirPath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static class OutboxEntry {
        public String matchId;
        public String jsonPayload;

        public OutboxEntry(String matchId, String jsonPayload) {
            this.matchId = matchId;
            this.jsonPayload = jsonPayload;
        }
    }

    /**
     * Write result record atomically to local disk storage using temp file + rename.
     * Requirements: 9.1, 9.2
     */
    public synchronized boolean savePendingResult(String matchId, Object saveMatchRequest) {
        try {
            String json = objectMapper.writeValueAsString(saveMatchRequest);
            File tempFile = new File(storageDir, matchId + ".tmp");
            File targetFile = new File(storageDir, matchId + ".json");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }

            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            logger.info("Outbox record saved for matchId: {}", matchId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to write result to outbox for matchId {}: {}", matchId, e.getMessage());
            return false;
        }
    }

    public synchronized List<OutboxEntry> getPendingResults() {
        List<OutboxEntry> list = new ArrayList<>();
        File[] files = storageDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    String matchId = file.getName().replace(".json", "");
                    list.add(new OutboxEntry(matchId, json));
                } catch (IOException e) {
                    logger.error("Error reading outbox file {}: {}", file.getName(), e.getMessage());
                }
            }
        }
        return list;
    }

    public synchronized void removeResult(String matchId) {
        File file = new File(storageDir, matchId + ".json");
        if (file.exists()) {
            file.delete();
            logger.info("Removed outbox record for matchId: {}", matchId);
        }
    }
}
