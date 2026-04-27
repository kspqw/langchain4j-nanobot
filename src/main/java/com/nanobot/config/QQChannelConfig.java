package com.nanobot.config;

import java.util.List;

public record QQChannelConfig(
    boolean enabled,
    String appId,
    String secret,
    List<String> allowFrom
) {
    public QQChannelConfig() { this(false, "", "", List.of()); }
}