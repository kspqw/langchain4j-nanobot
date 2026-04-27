package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for EmailChannel
 */
public class EmailChannel extends BaseChannel {
    private final boolean enabled;
    private final String imapHost;
    private final List<String> allowFrom;

    public EmailChannel(EmailChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.imapHost = config.imapHost();
        this.allowFrom = new ArrayList<>(config.allowFrom());
    }

    @Override
    public String name() { return "email"; }

    @Override
    public void start() {
        System.out.println("Email channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Email send: " + msg.content());
    }
}