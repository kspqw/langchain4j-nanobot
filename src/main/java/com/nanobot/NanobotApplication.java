package com.nanobot;

import com.nanobot.bus.MessageBus;
import com.nanobot.config.AppConfig;
import com.nanobot.config.ConfigLoader;
import com.nanobot.agent.AgentLoop;
import com.nanobot.channels.ChannelManager;
import com.nanobot.providers.ChatModelFactory;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanobotApplication {
    private static final Logger logger = LoggerFactory.getLogger(NanobotApplication.class);

    public static void main(String[] args) {
        logger.info("nanobot starting...");

        AppConfig config = ConfigLoader.load();
        MessageBus bus = new MessageBus();

        ChatModel chatModel = ChatModelFactory.create(config);

        AgentLoop agent = new AgentLoop(bus, chatModel, config);
        ChannelManager channels = new ChannelManager(config, bus);

        // 虚拟线程：ChannelManager 运行在虚拟线程
        Thread.startVirtualThread(() -> {
            try {
                channels.startAll();
            } catch (Exception e) {
                logger.error("Channel start failed", e);
            }
        });

        // Agent 运行在主线程
        try {
            agent.run();
        } catch (Exception e) {
            logger.error("Agent loop error", e);
        }

        logger.info("nanobot stopped");
    }
}