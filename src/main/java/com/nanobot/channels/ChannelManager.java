package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.*;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;

public class ChannelManager {
    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private final AppConfig config;
    private final MessageBus bus;
    private final Map<String, BaseChannel> channels = new ConcurrentHashMap<>();
    private ExecutorService dispatchExecutor;

    public ChannelManager(AppConfig config, MessageBus bus) {
        this.config = config;
        this.bus = bus;
        initChannels();
    }

    private void initChannels() {
        // ===== 修复：使用 AppConfig 的公共通道访问器方法 =====

        // 获取通道配置（AppConfig 的公共方法返回的是包级私有类型，
        // 但这里我们在同一调用链中只取基本值，实际上这些 getXxxConfig()
        // 都返回了包级私有类型，这会导致同样的问题...）
        //
        // 关键洞察：问题不仅是"访问 record 的字段"，更是"跨包返回包级私有类型"。
        //
        // 解决方案：用 isChannelEnabled() 判断启用状态，
        // 然后直接用 getXxxConfig() 获取配置对象传给 Channel 构造函数。
        // AppConfig.getXxxConfig() 方法在 AppConfig 内部（同包）访问了内部 record，
        // 返回的类型本身是包级私有的，但 ChannelManager 只需要把返回值原样传给
        // 对应的 Channel 构造函数即可——Channel 构造函数在同包内可以接受这个类型。

        // 检查并初始化 Telegram
        if (config.isChannelEnabled("telegram")) {
            channels.put("telegram", new TelegramChannel(config.getTelegramConfig(), bus));
            logger.info("Telegram channel configured");
        }
        // 检查并初始化 Discord
        if (config.isChannelEnabled("discord")) {
            channels.put("discord", new DiscordChannel(config.getDiscordConfig(), bus));
            logger.info("Discord channel configured");
        }
        // 检查并初始化 WhatsApp
        if (config.isChannelEnabled("whatsapp")) {
            channels.put("whatsapp", new WhatsAppChannel(config.getWhatsAppConfig(), bus));
            logger.info("WhatsApp channel configured");
        }
        // 检查并初始化 飞书
        if (config.isChannelEnabled("feishu")) {
            channels.put("feishu", new FeishuChannel(config.getFeishuConfig(), bus));
            logger.info("Feishu channel configured");
        }
        // 检查并初始化 钉钉
        if (config.isChannelEnabled("dingtalk")) {
            channels.put("dingtalk", new DingTalkChannel(config.getDingTalkConfig(), bus));
            logger.info("DingTalk channel configured");
        }
        // 检查并初始化 Email
        if (config.isChannelEnabled("email")) {
            channels.put("email", new EmailChannel(config.getEmailConfig(), bus));
            logger.info("Email channel configured");
        }
        // 检查并初始化 Slack
        if (config.isChannelEnabled("slack")) {
            channels.put("slack", new SlackChannel(config.getSlackConfig(), bus));
            logger.info("Slack channel configured");
        }
        // 检查并初始化 QQ
        if (config.isChannelEnabled("qq")) {
            channels.put("qq", new QQChannel(config.getQQConfig(), bus));
            logger.info("QQ channel configured");
        }
        // 检查并初始化 Matrix
        if (config.isChannelEnabled("matrix")) {
            channels.put("matrix", new MatrixChannel(config.getMatrixConfig(), bus));
            logger.info("Matrix channel configured");
        }
        // 检查并初始化 Mochat
        if (config.isChannelEnabled("mochat")) {
            channels.put("mochat", new MochatChannel(config.getMochatConfig(), bus));
            logger.info("Mochat channel configured");
        }
        // 检查并初始化 Console
        if (config.isChannelEnabled("console")) {
            channels.put("console", new ConsoleChannel(config.getConsoleConfig(), bus));
            logger.info("Console channel configured");
        }
    }

    public void startAll() {
        if (channels.isEmpty()) {
            logger.warn("No channels enabled");
            return;
        }

        dispatchExecutor =  Executors.newCachedThreadPool();
        dispatchExecutor.submit(this::dispatchOutbound);

        for (Map.Entry<String, BaseChannel> e : channels.entrySet()) {
            String name = e.getKey();
            BaseChannel ch = e.getValue();
            logger.info("Starting {} channel", name);
            try {
                ch.start();
            } catch (Exception ex) {
                logger.error("Failed to start {}: {}", name, ex);
            }
        }
    }

    public void stopAll() {
        logger.info("Stopping all channels");
        for (Map.Entry<String, BaseChannel> e : channels.entrySet()) {
            try { e.getValue().stop(); } catch (Exception ex) { logger.error("Error stopping {}", e.getKey(), ex); }
        }
        if (dispatchExecutor != null) dispatchExecutor.shutdown();
    }

    private void dispatchOutbound() {
        while (true) {
            try {
                OutboundMessage msg = bus.consumeOutbound();
                BaseChannel ch = channels.get(msg.channel());
                if (ch != null) {
                    ch.send(msg);
                } else {
                    logger.warn("Unknown channel: {}", msg.channel());
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error("Error in outbound dispatcher", e);
            }
        }
    }

    public List<String> enabledChannels() { return new ArrayList<>(channels.keySet()); }
}