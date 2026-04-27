package com.nanobot.bus;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageBus {
    public final BlockingQueue<InboundMessage> inbound = new LinkedBlockingQueue<>();
    public final BlockingQueue<OutboundMessage> outbound = new LinkedBlockingQueue<>();

    public void publishInbound(InboundMessage msg) throws InterruptedException {
        inbound.put(msg);
    }

    public InboundMessage consumeInbound() throws InterruptedException {
        return inbound.take();
    }

    public void publishOutbound(OutboundMessage msg) throws InterruptedException {
        outbound.put(msg);
    }

    public OutboundMessage consumeOutbound() throws InterruptedException {
        return outbound.take();
    }

    public int inboundSize() { return inbound.size(); }
    public int outboundSize() { return outbound.size(); }
}