package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for FeishuChannel
 */
public class FeishuChannel extends BaseChannel {
    private final boolean enabled;
    private final String appId;
    private final List<String> allowFrom;

    public FeishuChannel(FeishuChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.appId = config.appId();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "feishu"; }

    @Override
    public void start() {
        System.out.println("Feishu channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Feishu send: " + msg.content());
    }
}