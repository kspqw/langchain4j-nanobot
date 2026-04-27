package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for QQChannel
 */
public class QQChannel extends BaseChannel {
    private final boolean enabled;
    private final String appId;
    private final List<String> allowFrom;

    public QQChannel(QQChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.appId = config.appId();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "qq"; }

    @Override
    public void start() {
        System.out.println("QQ channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("QQ send: " + msg.content());
    }
}