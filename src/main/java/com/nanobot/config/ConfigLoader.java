package com.nanobot.config;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ConfigLoader {
    public static AppConfig load() {
        Yaml yaml = new Yaml();
        try {
            Path configPath = Path.of("config.yaml");
            if (Files.exists(configPath)) {
                try (InputStream is = Files.newInputStream(configPath)) {
                    Map<String, Object> raw = yaml.load(is);
                    return parse(raw);
                }
            }
            try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("application.yaml")) {
                if (is != null) {
                    Map<String, Object> raw = yaml.load(is);
                    return parse(raw);
                }
            }
            return defaultConfig();
        } catch (Exception e) {
            System.err.println("Failed to load config, using defaults: " + e.getMessage());
            return defaultConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private static AppConfig parse(Map<String, Object> raw) {
        try {
            if (raw == null || raw.isEmpty()) {
                return defaultConfig();
            }

            // 解析 agents.defaults
            Map<String, Object> agents = safeCast(raw.get("agents"), Map.class);
            Map<String, Object> defaults = agents != null ? safeCast(agents.get("defaults"), Map.class) : null;

            String workspace = defaults != null ? getString(defaults, "workspace", "~/.nanobot/workspace") : "~/.nanobot/workspace";
            String model = defaults != null ? getString(defaults, "model", "anthropic/claude-opus-4-5") : "anthropic/claude-opus-4-5";
            String provider = defaults != null ? getString(defaults, "provider", "auto") : "auto";
            int maxTokens = defaults != null ? getInt(defaults, "maxTokens", 8192) : 8192;
            double temperature = defaults != null ? getDouble(defaults, "temperature", 0.1) : 0.1;
            int maxToolIterations = defaults != null ? getInt(defaults, "maxToolIterations", 40) : 40;
            int memoryWindow = defaults != null ? getInt(defaults, "memoryWindow", 100) : 100;
            String reasoningEffort = defaults != null ? (String) defaults.get("reasoningEffort") : null;

            AgentDefaults agentDefaults = new AgentDefaults(
                    workspace, model, provider, maxTokens, temperature,
                    maxToolIterations, memoryWindow, reasoningEffort
            );

            // 解析 channels
            Map<String, Object> channels = safeCast(raw.get("channels"), Map.class);
            boolean sendProgress = channels != null ? getBoolean(channels, "sendProgress", true) : true;
            boolean sendToolHints = channels != null ? getBoolean(channels, "sendToolHints", false) : false;

            TelegramChannelConfig telegram = parseTelegramConfig(channels);
            DiscordChannelConfig discord = parseDiscordConfig(channels);
            WhatsAppChannelConfig whatsapp = parseWhatsAppConfig(channels);
            FeishuChannelConfig feishu = parseFeishuConfig(channels);
            DingTalkChannelConfig dingtalk = parseDingTalkConfig(channels);
            EmailChannelConfig email = parseEmailConfig(channels);
            SlackChannelConfig slack = parseSlackConfig(channels);
            QQChannelConfig qq = parseQQConfig(channels);
            MatrixChannelConfig matrix = parseMatrixConfig(channels);
            MochatChannelConfig mochat = parseMochatConfig(channels);
            ConsoleChannelConfig console = parseConsoleConfig(channels);

            ChannelsConfig channelsConfig = new ChannelsConfig(
                    telegram, discord, whatsapp, feishu, dingtalk, email, slack, qq, matrix, mochat,
                    console, sendProgress, sendToolHints
            );

            // 解析 providers
            Map<String, Object> providersRaw = safeCast(raw.get("providers"), Map.class);
            Map<String, ProviderConfigEntry> providers = new HashMap<>();
            if (providersRaw != null) {
                for (Map.Entry<String, Object> entry : providersRaw.entrySet()) {
                    Map<String, Object> providerConfig = safeCast(entry.getValue(), Map.class);
                    if (providerConfig != null) {
                        String apiKey = getString(providerConfig, "apiKey", "");
                        String apiBase = (String) providerConfig.get("apiBase");
                        providers.put(entry.getKey(), new ProviderConfigEntry(apiKey, apiBase, Map.of()));
                    }
                }
            }

            ProvidersConfig providersConfig = new ProvidersConfig(providers);

            // 解析 gateway
            Map<String, Object> gateway = safeCast(raw.get("gateway"), Map.class);
            String host = gateway != null ? getString(gateway, "host", "0.0.0.0") : "0.0.0.0";
            int port = gateway != null ? getInt(gateway, "port", 18790) : 18790;
            GatewayConfig gatewayConfig = new GatewayConfig(host, port);

            // 解析 tools
            Map<String, Object> tools = safeCast(raw.get("tools"), Map.class);
            Map<String, Object> webTools = tools != null ? safeCast(tools.get("web"), Map.class) : null;
            String webApiKey = webTools != null ? getString(webTools, "apiKey", "") : "";
            String webProxy = webTools != null ? (String) webTools.get("proxy") : null;
            WebToolsConfig webConfig = new WebToolsConfig(webApiKey, webProxy);

            Map<String, Object> execTools = tools != null ? safeCast(tools.get("exec"), Map.class) : null;
            int execTimeout = execTools != null ? getInt(execTools, "timeout", 60) : 60;
            String pathAppend = execTools != null ? getString(execTools, "pathAppend", "") : "";
            ExecToolConfig execConfig = new ExecToolConfig(execTimeout, pathAppend);

            boolean restrictToWorkspace = tools != null ? getBoolean(tools, "restrictToWorkspace", false) : false;
            ToolsConfig toolsConfig = new ToolsConfig(webConfig, execConfig, restrictToWorkspace);
            AgentsConfig agentsConfig = new AgentsConfig(agentDefaults);

            return new AppConfig(agentsConfig, channelsConfig, providersConfig, gatewayConfig, toolsConfig);
        } catch (Exception e) {
            System.err.println("Error parsing config: " + e.getMessage());
            e.printStackTrace();
            return defaultConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T safeCast(Object obj, Class<T> clazz) {
        if (obj != null && clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return null;
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static TelegramChannelConfig parseTelegramConfig(Map<String, Object> channels) {
        if (channels == null) return new TelegramChannelConfig();
        Map<String, Object> config = safeCast(channels.get("telegram"), Map.class);
        if (config == null) return new TelegramChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String token = getString(config, "token", "");
        List<String> allowFrom = getStringList(config, "allowFrom");
        String proxy = getString(config, "proxy", null);
        boolean replyToMessage = getBoolean(config, "replyToMessage", false);

        return new TelegramChannelConfig(enabled, token, allowFrom, proxy, replyToMessage);
    }

    private static DiscordChannelConfig parseDiscordConfig(Map<String, Object> channels) {
        if (channels == null) return new DiscordChannelConfig();
        Map<String, Object> config = safeCast(channels.get("discord"), Map.class);
        if (config == null) return new DiscordChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String token = getString(config, "token", "");
        List<String> allowFrom = getStringList(config, "allowFrom");
        String gatewayUrl = getString(config, "gatewayUrl", null);
        int intents = getInt(config, "intents", 0);
        String groupPolicy = getString(config, "groupPolicy", null);

        return new DiscordChannelConfig(enabled, token, allowFrom, gatewayUrl, intents, groupPolicy);
    }

    private static WhatsAppChannelConfig parseWhatsAppConfig(Map<String, Object> channels) {
        if (channels == null) return new WhatsAppChannelConfig();
        Map<String, Object> config = safeCast(channels.get("whatsapp"), Map.class);
        if (config == null) return new WhatsAppChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String bridgeUrl = getString(config, "bridgeUrl", "ws://localhost:3001");
        String bridgeToken = getString(config, "bridgeToken", "");
        List<String> allowFrom = getStringList(config, "allowFrom");

        return new WhatsAppChannelConfig(enabled, bridgeUrl, bridgeToken, allowFrom);
    }

    private static FeishuChannelConfig parseFeishuConfig(Map<String, Object> channels) {
        if (channels == null) return new FeishuChannelConfig();
        Map<String, Object> config = safeCast(channels.get("feishu"), Map.class);
        if (config == null) return new FeishuChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String appId = getString(config, "appId", "");
        String appSecret = getString(config, "appSecret", "");
        String encryptKey = getString(config, "encryptKey", null);
        String verificationToken = getString(config, "verificationToken", null);
        List<String> allowFrom = getStringList(config, "allowFrom");
        String reactEmoji = getString(config, "reactEmoji", null);

        return new FeishuChannelConfig(enabled, appId, appSecret, encryptKey, verificationToken, allowFrom, reactEmoji);
    }

    private static DingTalkChannelConfig parseDingTalkConfig(Map<String, Object> channels) {
        if (channels == null) return new DingTalkChannelConfig();
        Map<String, Object> config = safeCast(channels.get("dingtalk"), Map.class);
        if (config == null) return new DingTalkChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String clientId = getString(config, "clientId", "");
        String clientSecret = getString(config, "clientSecret", "");
        List<String> allowFrom = getStringList(config, "allowFrom");

        return new DingTalkChannelConfig(enabled, clientId, clientSecret, allowFrom);
    }

    private static EmailChannelConfig parseEmailConfig(Map<String, Object> channels) {
        if (channels == null) return new EmailChannelConfig();
        Map<String, Object> config = safeCast(channels.get("email"), Map.class);
        if (config == null) return new EmailChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String imapHost = getString(config, "imapHost", "");
        int imapPort = getInt(config, "imapPort", 993);
        String imapUsername = getString(config, "imapUsername", "");
        String imapPassword = getString(config, "imapPassword", "");
        String smtpHost = getString(config, "smtpHost", "");
        int smtpPort = getInt(config, "smtpPort", 587);
        String smtpUsername = getString(config, "smtpUsername", "");
        String smtpPassword = getString(config, "smtpPassword", "");
        List<String> allowFrom = getStringList(config, "allowFrom");

        return new EmailChannelConfig(enabled, imapHost, imapPort, imapUsername, imapPassword,
                smtpHost, smtpPort, smtpUsername, smtpPassword, allowFrom);
    }

    private static SlackChannelConfig parseSlackConfig(Map<String, Object> channels) {
        if (channels == null) return new SlackChannelConfig();
        Map<String, Object> config = safeCast(channels.get("slack"), Map.class);
        if (config == null) return new SlackChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String botToken = getString(config, "botToken", "");
        String appToken = getString(config, "appToken", "");
        List<String> allowFrom = getStringList(config, "allowFrom");
        String groupPolicy = getString(config, "groupPolicy", null);
        List<String> groupAllowFrom = getStringList(config, "groupAllowFrom");

        return new SlackChannelConfig(enabled, botToken, appToken, allowFrom, groupPolicy, groupAllowFrom);
    }

    private static QQChannelConfig parseQQConfig(Map<String, Object> channels) {
        if (channels == null) return new QQChannelConfig();
        Map<String, Object> config = safeCast(channels.get("qq"), Map.class);
        if (config == null) return new QQChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String appId = getString(config, "appId", "");
        String secret = getString(config, "secret", "");
        List<String> allowFrom = getStringList(config, "allowFrom");

        return new QQChannelConfig(enabled, appId, secret, allowFrom);
    }

    private static MatrixChannelConfig parseMatrixConfig(Map<String, Object> channels) {
        if (channels == null) return new MatrixChannelConfig();
        Map<String, Object> config = safeCast(channels.get("matrix"), Map.class);
        if (config == null) return new MatrixChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String homeserver = getString(config, "homeserver", "https://matrix.org");
        String accessToken = getString(config, "accessToken", "");
        String userId = getString(config, "userId", null);
        String deviceId = getString(config, "deviceId", null);
        boolean e2eeEnabled = getBoolean(config, "e2eeEnabled", false);
        List<String> allowFrom = getStringList(config, "allowFrom");
        String groupPolicy = getString(config, "groupPolicy", null);
        List<String> groupAllowFrom = getStringList(config, "groupAllowFrom");

        return new MatrixChannelConfig(enabled, homeserver, accessToken, userId, deviceId,
                e2eeEnabled, allowFrom, groupPolicy, groupAllowFrom);
    }

    private static MochatChannelConfig parseMochatConfig(Map<String, Object> channels) {
        if (channels == null) return new MochatChannelConfig();
        Map<String, Object> config = safeCast(channels.get("mochat"), Map.class);
        if (config == null) return new MochatChannelConfig();

        boolean enabled = getBoolean(config, "enabled", false);
        String baseUrl = getString(config, "baseUrl", "https://mochat.io");
        String socketUrl = getString(config, "socketUrl", null);
        String socketPath = getString(config, "socketPath", null);
        List<String> allowFrom = getStringList(config, "allowFrom");

        return new MochatChannelConfig(enabled, baseUrl, socketUrl, socketPath, allowFrom);
    }

    private static ConsoleChannelConfig parseConsoleConfig(Map<String, Object> channels) {
        if (channels == null) return new ConsoleChannelConfig();
        Map<String, Object> config = safeCast(channels.get("console"), Map.class);
        if (config == null) return new ConsoleChannelConfig();

        boolean enabled = getBoolean(config, "enabled", true);
        String botName = getString(config, "botName", "nanobot");
        List<String> allowFrom = getStringList(config, "allowFrom");

        return new ConsoleChannelConfig(enabled, botName, allowFrom);
    }

    private static AppConfig defaultConfig() {
        return new AppConfig(
                new AgentsConfig(new AgentDefaults()),
                new ChannelsConfig(),
                new ProvidersConfig(Map.of()),
                new GatewayConfig(),
                new ToolsConfig()
        );
    }
}
