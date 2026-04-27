package com.nanobot.config;

import java.util.List;

public record FeishuChannelConfig(
    boolean enabled,
    String appId,
    String appSecret,
    String encryptKey,
    String verificationToken,
    List<String> allowFrom,
    String reactEmoji
) {
    public FeishuChannelConfig() { this(false, "", "", "", "", List.of(), "THUMBSUP"); }
}