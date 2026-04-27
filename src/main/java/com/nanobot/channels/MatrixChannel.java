package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for MatrixChannel
 */
public class MatrixChannel extends BaseChannel {
    private final boolean enabled;
    private final String homeserver;
    private final List<String> allowFrom;

    public MatrixChannel(MatrixChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.homeserver = config.homeserver();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "matrix"; }

    @Override
    public void start() {
        System.out.println("Matrix channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Matrix send: " + msg.content());
    }
}