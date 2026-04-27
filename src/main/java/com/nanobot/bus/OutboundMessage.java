package com.nanobot.bus;

import java.util.List;
import java.util.Map;

public record OutboundMessage(
    String channel,
    String chatId,
    String content,
    String replyTo,
    List<String> media,
    Map<String, Object> metadata
) {
    public OutboundMessage(String channel, String chatId, String content) {
        this(channel, chatId, content, null, List.of(), Map.of());
    }
}