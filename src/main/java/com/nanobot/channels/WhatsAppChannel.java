package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for WhatsAppChannel
 */
public class WhatsAppChannel extends BaseChannel {
    private final boolean enabled;
    private final String bridgeUrl;
    private final List<String> allowFrom;

    public WhatsAppChannel(WhatsAppChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.bridgeUrl = config.bridgeUrl();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "whatsapp"; }

    @Override
    public void start() {
        System.out.println("WhatsApp channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("WhatsApp send: " + msg.content());
    }
}