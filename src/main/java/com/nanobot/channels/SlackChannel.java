package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for SlackChannel
 */
public class SlackChannel extends BaseChannel {
    private final boolean enabled;
    private final String botToken;
    private final List<String> allowFrom;

    public SlackChannel(SlackChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.botToken = config.botToken();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "slack"; }

    @Override
    public void start() {
        System.out.println("Slack channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Slack send: " + msg.content());
    }
}