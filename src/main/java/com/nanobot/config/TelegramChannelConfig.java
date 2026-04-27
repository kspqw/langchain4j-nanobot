package com.nanobot.config;

import java.util.List;

public record TelegramChannelConfig(
    boolean enabled,
    String token,
    List<String> allowFrom,
    String proxy,
    boolean replyToMessage
) {
    public TelegramChannelConfig() { this(false, "", List.of(), null, false); }
}