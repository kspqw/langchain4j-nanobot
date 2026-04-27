package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import com.nanobot.bus.*;

public class MessageTool {
    private final MessageBus bus;
    private String currentChannel;
    private String currentChatId;

    public MessageTool(MessageBus bus) { this.bus = bus; }

    public void setContext(String channel, String chatId) {
        this.currentChannel = channel;
        this.currentChatId = chatId;
    }

    @Tool("Send a message to a chat channel")
    public String sendMessage(@P("Channel name (e.g. telegram, discord)") String channel,
                              @P("Chat ID") String chatId,
                              @P("Message content") String content) {
        try {
            bus.publishOutbound(new OutboundMessage(channel, chatId, content));
            return "Message sent to " + channel + ":" + chatId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Failed to send message";
        }
    }
}