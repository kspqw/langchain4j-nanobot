package com.nanobot.config;

import java.util.List;

public record DingTalkChannelConfig(
    boolean enabled,
    String clientId,
    String clientSecret,
    List<String> allowFrom
) {
    public DingTalkChannelConfig() { this(false, "", "", List.of()); }
}