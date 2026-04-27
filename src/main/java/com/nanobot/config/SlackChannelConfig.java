package com.nanobot.config;

import java.util.List;

public record SlackChannelConfig(
    boolean enabled,
    String botToken,
    String appToken,
    List<String> allowFrom,
    String groupPolicy,
    List<String> groupAllowFrom
) {
    public SlackChannelConfig() { this(false, "", "", List.of(), "mention", List.of()); }
}