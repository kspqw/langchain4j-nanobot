package com.nanobot.config;

import java.util.List;

public record EmailChannelConfig(
    boolean enabled,
    String imapHost,
    int imapPort,
    String imapUsername,
    String imapPassword,
    String smtpHost,
    int smtpPort,
    String smtpUsername,
    String smtpPassword,
    List<String> allowFrom
) {
    public EmailChannelConfig() { this(false, "", 993, "", "", "", 587, "", "", List.of()); }
}