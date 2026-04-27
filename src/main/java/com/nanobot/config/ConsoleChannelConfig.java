package com.nanobot.config;

import java.util.List;

public record ConsoleChannelConfig(
        boolean enabled,
        String botName,
        List<String> allowFrom
) {
    public ConsoleChannelConfig() {
        this(true, "nanobot", List.of("*"));
    }
}