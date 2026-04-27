package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for DingTalkChannel
 */
public class DingTalkChannel extends BaseChannel {
    private final boolean enabled;
    private final String clientId;
    private final List<String> allowFrom;

    public DingTalkChannel(DingTalkChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.clientId = config.clientId();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "dingtalk"; }

    @Override
    public void start() {
        System.out.println("DingTalk channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("DingTalk send: " + msg.content());
    }
}