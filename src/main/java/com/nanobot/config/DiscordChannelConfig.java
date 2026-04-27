package com.nanobot.config;

import java.util.List;

public record DiscordChannelConfig(
    boolean enabled,
    String token,
    List<String> allowFrom,
    String gatewayUrl,
    int intents,
    String groupPolicy
) {
    public DiscordChannelConfig() { this(false, "", List.of(), "wss://gateway.discord.gg/?v=10&encoding=json", 37377, "mention"); }
}