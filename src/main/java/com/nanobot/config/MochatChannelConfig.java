package com.nanobot.config;

import java.util.List;

public record MochatChannelConfig(
    boolean enabled,
    String baseUrl,
    String socketUrl,
    String socketPath,
    List<String> allowFrom
) {
    public MochatChannelConfig() { this(false, "https://mochat.io", "", "", List.of()); }
}