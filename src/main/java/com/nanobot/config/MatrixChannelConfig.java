package com.nanobot.config;

import java.util.List;

public record MatrixChannelConfig(
    boolean enabled,
    String homeserver,
    String accessToken,
    String userId,
    String deviceId,
    boolean e2eeEnabled,
    List<String> allowFrom,
    String groupPolicy,
    List<String> groupAllowFrom
) {
    public MatrixChannelConfig() { this(false, "https://matrix.org", "", "", "", true, List.of(), "open", List.of()); }
}