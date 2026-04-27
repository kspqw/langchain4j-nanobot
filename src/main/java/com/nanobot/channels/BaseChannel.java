package com.nanobot.channels;

import com.nanobot.bus.*;
import java.util.*;

public abstract class BaseChannel {
    protected final Object config;
    protected final MessageBus bus;
    protected volatile boolean running = false;

    public abstract String name();

    protected BaseChannel(Object config, MessageBus bus) {
        this.config = config;
        this.bus = bus;
    }

    public abstract void start();
    public abstract void stop();
    public abstract void send(OutboundMessage msg);

    protected boolean isAllowed(Object senderId, List<String> allowFrom) {
        if (allowFrom == null || allowFrom.isEmpty()) return false;
        if (allowFrom.contains("*")) return true;
        String id = String.valueOf(senderId);
        return allowFrom.contains(id);
    }

    protected void publishInbound(String channel, Object senderId, Object chatId,
                                   String content, List<String> media, Map<String, Object> metadata) {
        try {
            bus.publishInbound(new InboundMessage(channel, String.valueOf(senderId),
                String.valueOf(chatId), content, java.time.Instant.now(), media != null ? media : List.of(),
                metadata != null ? metadata : Map.of(), null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() { return running; }
}