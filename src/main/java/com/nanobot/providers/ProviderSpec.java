package com.nanobot.providers;

public record ProviderSpec(
    String name,
    String[] keywords,
    String envKey,
    String displayName,
    String litellmPrefix,
    String[] skipPrefixes,
    boolean isGateway,
    boolean isLocal,
    String detectByKeyPrefix,
    String detectByBaseKeyword,
    String defaultApiBase,
    boolean stripModelPrefix
) {
    public static ProviderSpec of(String name, String... keywords) {
        return new ProviderSpec(name, keywords, "", name, "", new String[0],
            false, false, "", "", "", false);
    }
}