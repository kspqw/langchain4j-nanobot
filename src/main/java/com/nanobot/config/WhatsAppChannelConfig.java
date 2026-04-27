package com.nanobot.config;

import java.util.List;

public record WhatsAppChannelConfig(
    boolean enabled,
    String bridgeUrl,
    String bridgeToken,
    List<String> allowFrom
) {
    public WhatsAppChannelConfig() { this(false, "ws://localhost:3001", "", List.of()); }
}