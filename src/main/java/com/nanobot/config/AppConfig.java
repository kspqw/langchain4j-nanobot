package com.nanobot.config;

import java.util.List;
import java.util.Map;

// 导入单独文件中的 ChannelConfig 定义
import com.nanobot.config.TelegramChannelConfig;
import com.nanobot.config.DiscordChannelConfig;
import com.nanobot.config.WhatsAppChannelConfig;
import com.nanobot.config.FeishuChannelConfig;
import com.nanobot.config.DingTalkChannelConfig;
import com.nanobot.config.EmailChannelConfig;
import com.nanobot.config.SlackChannelConfig;
import com.nanobot.config.QQChannelConfig;
import com.nanobot.config.MatrixChannelConfig;
import com.nanobot.config.MochatChannelConfig;
import com.nanobot.config.ConsoleChannelConfig;

public record AppConfig(
        AgentsConfig agents,
        ChannelsConfig channels,
        ProvidersConfig providers,
        GatewayConfig gateway,
        ToolsConfig tools
) {
    // ========== Provider 访问器 ==========

    public Map<String, ProviderConfigEntry> getProviderEntries() {
        return providers.providers();
    }

    public ProviderConfigEntry getProviderEntry(String providerName) {
        Map<String, ProviderConfigEntry> entries = getProviderEntries();
        return entries != null ? entries.get(providerName) : null;
    }

    public String getApiKeyForProvider(String providerName) {
        ProviderConfigEntry entry = getProviderEntry(providerName);
        return entry != null ? entry.apiKey() : "";
    }

    public String getApiBaseForProvider(String providerName) {
        ProviderConfigEntry entry = getProviderEntry(providerName);
        return entry != null ? entry.apiBase() : null;
    }

    // ========== Agent 默认值访问器 ==========

    public String getDefaultModel() { return agents.defaults().model(); }
    public String getDefaultProvider() { return agents.defaults().provider(); }
    public String getWorkspace() { return agents.defaults().workspace(); }
    public int getMaxTokens() { return agents.defaults().maxTokens(); }
    public double getTemperature() { return agents.defaults().temperature(); }
    public int getMaxToolIterations() { return agents.defaults().maxToolIterations(); }
    public int getMemoryWindow() { return agents.defaults().memoryWindow(); }
    public String getReasoningEffort() { return agents.defaults().reasoningEffort(); }

    // ========== Channel 访问器（返回基本类型，不返回 Record）==========

    public boolean isChannelEnabled(String channelName) {
        return switch (channelName.toLowerCase()) {
            case "telegram"  -> channels.telegram().enabled();
            case "discord"   -> channels.discord().enabled();
            case "whatsapp"  -> channels.whatsapp().enabled();
            case "feishu"    -> channels.feishu().enabled();
            case "dingtalk"  -> channels.dingtalk().enabled();
            case "email"     -> channels.email().enabled();
            case "slack"     -> channels.slack().enabled();
            case "qq"        -> channels.qq().enabled();
            case "matrix"    -> channels.matrix().enabled();
            case "mochat"    -> channels.mochat().enabled();
            case "console"   -> channels.console().enabled();
            default -> false;
        };
    }

    // ========== 返回完整配置对象的方法 ==========
    public TelegramChannelConfig getTelegramConfig() { return channels.telegram(); }
    public DiscordChannelConfig getDiscordConfig() { return channels.discord(); }
    public WhatsAppChannelConfig getWhatsAppConfig() { return channels.whatsapp(); }
    public FeishuChannelConfig getFeishuConfig() { return channels.feishu(); }
    public DingTalkChannelConfig getDingTalkConfig() { return channels.dingtalk(); }
    public EmailChannelConfig getEmailConfig() { return channels.email(); }
    public SlackChannelConfig getSlackConfig() { return channels.slack(); }
    public QQChannelConfig getQQConfig() { return channels.qq(); }
    public MatrixChannelConfig getMatrixConfig() { return channels.matrix(); }
    public MochatChannelConfig getMochatConfig() { return channels.mochat(); }
    public ConsoleChannelConfig getConsoleConfig() { return channels.console(); }

    // Telegram 字段
    public String getTelegramToken() { return channels.telegram().token(); }
    public List<String> getTelegramAllowFrom() { return channels.telegram().allowFrom(); }
    public String getTelegramProxy() { return channels.telegram().proxy(); }
    public boolean getTelegramReplyToMessage() { return channels.telegram().replyToMessage(); }

    // Discord 字段
    public String getDiscordToken() { return channels.discord().token(); }
    public List<String> getDiscordAllowFrom() { return channels.discord().allowFrom(); }
    public String getDiscordGatewayUrl() { return channels.discord().gatewayUrl(); }
    public int getDiscordIntents() { return channels.discord().intents(); }
    public String getDiscordGroupPolicy() { return channels.discord().groupPolicy(); }

    // WhatsApp 字段
    public String getWhatsAppBridgeUrl() { return channels.whatsapp().bridgeUrl(); }
    public String getWhatsAppBridgeToken() { return channels.whatsapp().bridgeToken(); }
    public List<String> getWhatsAppAllowFrom() { return channels.whatsapp().allowFrom(); }

    // 飞书 字段
    public String getFeishuAppId() { return channels.feishu().appId(); }
    public String getFeishuAppSecret() { return channels.feishu().appSecret(); }
    public String getFeishuEncryptKey() { return channels.feishu().encryptKey(); }
    public String getFeishuVerificationToken() { return channels.feishu().verificationToken(); }
    public List<String> getFeishuAllowFrom() { return channels.feishu().allowFrom(); }
    public String getFeishuReactEmoji() { return channels.feishu().reactEmoji(); }

    // 钉钉 字段
    public String getDingTalkClientId() { return channels.dingtalk().clientId(); }
    public String getDingTalkClientSecret() { return channels.dingtalk().clientSecret(); }
    public List<String> getDingTalkAllowFrom() { return channels.dingtalk().allowFrom(); }

    // Email 字段
    public String getEmailImapHost() { return channels.email().imapHost(); }
    public int getEmailImapPort() { return channels.email().imapPort(); }
    public String getEmailImapUsername() { return channels.email().imapUsername(); }
    public String getEmailImapPassword() { return channels.email().imapPassword(); }
    public String getEmailSmtpHost() { return channels.email().smtpHost(); }
    public int getEmailSmtpPort() { return channels.email().smtpPort(); }
    public String getEmailSmtpUsername() { return channels.email().smtpUsername(); }
    public String getEmailSmtpPassword() { return channels.email().smtpPassword(); }
    public List<String> getEmailAllowFrom() { return channels.email().allowFrom(); }

    // Slack 字段
    public String getSlackBotToken() { return channels.slack().botToken(); }
    public String getSlackAppToken() { return channels.slack().appToken(); }
    public List<String> getSlackAllowFrom() { return channels.slack().allowFrom(); }
    public String getSlackGroupPolicy() { return channels.slack().groupPolicy(); }
    public List<String> getSlackGroupAllowFrom() { return channels.slack().groupAllowFrom(); }

    // QQ 字段
    public String getQQAppId() { return channels.qq().appId(); }
    public String getQQSecret() { return channels.qq().secret(); }
    public List<String> getQQAllowFrom() { return channels.qq().allowFrom(); }

    // Matrix 字段
    public String getMatrixHomeserver() { return channels.matrix().homeserver(); }
    public String getMatrixAccessToken() { return channels.matrix().accessToken(); }
    public String getMatrixUserId() { return channels.matrix().userId(); }
    public String getMatrixDeviceId() { return channels.matrix().deviceId(); }
    public boolean getMatrixE2eeEnabled() { return channels.matrix().e2eeEnabled(); }
    public List<String> getMatrixAllowFrom() { return channels.matrix().allowFrom(); }
    public String getMatrixGroupPolicy() { return channels.matrix().groupPolicy(); }
    public List<String> getMatrixGroupAllowFrom() { return channels.matrix().groupAllowFrom(); }

    // Mochat 字段
    public String getMochatBaseUrl() { return channels.mochat().baseUrl(); }
    public String getMochatSocketUrl() { return channels.mochat().socketUrl(); }
    public String getMochatSocketPath() { return channels.mochat().socketPath(); }
    public List<String> getMochatAllowFrom() { return channels.mochat().allowFrom(); }

    // 通用 Channel 字段
    public boolean getSendProgress() { return channels.sendProgress(); }
    public boolean getSendToolHints() { return channels.sendToolHints(); }

    // ========== Gateway 访问器 ==========
    public String getGatewayHost() { return gateway.host(); }
    public int getGatewayPort() { return gateway.port(); }

    // ========== Tools 访问器 ==========
    public String getWebApiKey() { return tools.web().apiKey(); }
    public String getWebProxy() { return tools.web().proxy(); }
    public int getExecTimeout() { return tools.exec().timeout(); }
    public String getExecPathAppend() { return tools.exec().pathAppend(); }
    public boolean getRestrictToWorkspace() { return tools.restrictToWorkspace(); }
}

// ========== 内部 Record（全部包级私有）==========

record AgentsConfig(AgentDefaults defaults) {}

record AgentDefaults(
        String workspace,
        String model,
        String provider,
        int maxTokens,
        double temperature,
        int maxToolIterations,
        int memoryWindow,
        String reasoningEffort
) {
    public AgentDefaults() {
        this("~/.nanobot/workspace", "anthropic/claude-opus-4-5", "auto", 8192, 0.1, 40, 100, null);
    }
}

record ChannelsConfig(
        TelegramChannelConfig telegram,
        DiscordChannelConfig discord,
        WhatsAppChannelConfig whatsapp,
        FeishuChannelConfig feishu,
        DingTalkChannelConfig dingtalk,
        EmailChannelConfig email,
        SlackChannelConfig slack,
        QQChannelConfig qq,
        MatrixChannelConfig matrix,
        MochatChannelConfig mochat,
        ConsoleChannelConfig console,
        boolean sendProgress,
        boolean sendToolHints
) {
    public ChannelsConfig() {
        this(new TelegramChannelConfig(), new DiscordChannelConfig(),
                new WhatsAppChannelConfig(), new FeishuChannelConfig(),
                new DingTalkChannelConfig(), new EmailChannelConfig(),
                new SlackChannelConfig(), new QQChannelConfig(),
                new MatrixChannelConfig(), new MochatChannelConfig(),
                new ConsoleChannelConfig(),
                true, false);
    }
}

record ProvidersConfig(Map<String, ProviderConfigEntry> providers) {}

record ProviderConfigEntry(
        String apiKey,
        String apiBase,
        Map<String, String> extraHeaders
) {
    public ProviderConfigEntry() { this("", null, Map.of()); }
    public ProviderConfigEntry(String apiKey) { this(apiKey, null, Map.of()); }
}

record GatewayConfig(String host, int port) {
    public GatewayConfig() { this("0.0.0.0", 18790); }
}

record ToolsConfig(
        WebToolsConfig web,
        ExecToolConfig exec,
        boolean restrictToWorkspace
) {
    public ToolsConfig() { this(new WebToolsConfig(), new ExecToolConfig(), false); }
}

record WebToolsConfig(String apiKey, String proxy) {
    public WebToolsConfig() { this("", null); }
}

record ExecToolConfig(int timeout, String pathAppend) {
    public ExecToolConfig() { this(60, ""); }
}