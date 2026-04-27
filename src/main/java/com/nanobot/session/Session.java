package com.nanobot.session;

import java.time.Instant;
import java.util.*;

public class Session {
    public String key;
    public List<Map<String, Object>> messages = new ArrayList<>();
    public int lastConsolidated = 0;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    public Session(String key) { this.key = key; }

    public void clear() {
        messages.clear();
        lastConsolidated = 0;
        updatedAt = Instant.now();
    }

    public List<Map<String, Object>> getHistory(int maxMessages) {
        if (messages.isEmpty()) return List.of();
        int start = Math.max(0, messages.size() - maxMessages);
        return messages.subList(start, messages.size());
    }
}