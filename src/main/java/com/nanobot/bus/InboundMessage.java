package com.nanobot.bus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InboundMessage(
    String channel,
    String senderId,
    String chatId,
    String content,
    Instant timestamp,
    List<String> media,
    Map<String, Object> metadata,
    String sessionKeyOverride
) {
    public InboundMessage(String channel, String senderId, String chatId, String content) {
        this(channel, senderId, chatId, content, Instant.now(), List.of(), Map.of(), null);
    }

    public String sessionKey() {
        return sessionKeyOverride != null ? sessionKeyOverride : channel + ":" + chatId;
    }
}