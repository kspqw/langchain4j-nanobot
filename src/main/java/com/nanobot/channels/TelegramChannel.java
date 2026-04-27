package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import java.util.*;

/**
 * Stub for TelegramChannel - uses basic types
 */
public class TelegramChannel extends BaseChannel {
    private final boolean enabled;
    private final String token;
    private final List<String> allowFrom;
    private final String proxy;
    private final boolean replyToMessage;

    public TelegramChannel(TelegramChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.enabled = config.enabled();
        this.token = config.token();
        this.allowFrom = new ArrayList<>(config.allowFrom());
        this.proxy = config.proxy();
        this.replyToMessage = config.replyToMessage();
    }

    @Override
    public String name() { return "telegram"; }

    @Override
    public void start() {
        System.out.println("Telegram channel stub (enabled=" + enabled + ")");
    }

    @Override
    public void stop() {}

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("Telegram send: " + msg.content());
    }
}