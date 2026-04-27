package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for MochatChannel
 */
public class MochatChannel extends BaseChannel {
    private final boolean enabled;
    private final String baseUrl;
    private final List<String> allowFrom;

    public MochatChannel(MochatChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.baseUrl = config.baseUrl();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "mochat"; }

    @Override
    public void start() {
        System.out.println("Mochat channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Mochat send: " + msg.content());
    }
}