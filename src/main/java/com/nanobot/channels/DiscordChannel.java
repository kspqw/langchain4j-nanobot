package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for DiscordChannel
 */
public class DiscordChannel extends BaseChannel {
    private final boolean enabled;
    private final String token;
    private final List<String> allowFrom;

    public DiscordChannel(DiscordChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.token = config.token();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "discord"; }

    @Override
    public void start() {
        System.out.println("Discord channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Discord send: " + msg.content());
    }
}