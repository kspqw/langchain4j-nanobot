package com.nanobot.providers;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import com.nanobot.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.*;

public class ChatModelFactory {
    private static final Logger logger = LoggerFactory.getLogger(ChatModelFactory.class);

    public static ChatModel create(AppConfig config) {
        String model = config.getDefaultModel();
        ProviderSpec spec = resolveProvider(model, config);

        String effectiveModel = applyPrefix(model, spec);
        String apiKey = getApiKey(spec, config);
        String apiBase = getApiBase(spec, config);

        logger.info("Creating ChatModel: provider={}, model={}, apiBase={}", spec.name(), effectiveModel, apiBase);

        if ("azure_openai".equals(spec.name())) {
            AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .deploymentName(effectiveModel);

            if (apiBase != null && !apiBase.isEmpty()) {
                builder.endpoint(apiBase);
            }

            return builder.build();
        }

        if ("anthropic".equals(spec.name())) {
            return AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(effectiveModel)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        // 默认 OpenAI 兼容接口（覆盖 OpenRouter, SiliconFlow, DeepSeek, etc.）
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .modelName(effectiveModel)
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(120));

        if (apiBase != null && !apiBase.isEmpty()) {
            builder.baseUrl(apiBase);
        }

        return builder.build();
    }

    private static ProviderSpec resolveProvider(String model, AppConfig config) {
        String forced = config.getDefaultProvider();
        if (!"auto".equals(forced)) {
            for (ProviderSpec spec : ProviderRegistry.PROVIDERS) {
                if (spec.name().equals(forced)) return spec;
            }
        }
        ProviderSpec byModel = ProviderRegistry.findByModel(model);
        if (byModel != null) {
            String apiKey = config.getApiKeyForProvider(byModel.name());
            if (apiKey != null && !apiKey.isEmpty()) return byModel;
        }
        ProviderSpec gateway = ProviderRegistry.findGateway(null, null, null);
        if (gateway != null) {
            String apiKey = config.getApiKeyForProvider(gateway.name());
            if (apiKey != null && !apiKey.isEmpty()) return gateway;
        }
        return new ProviderSpec("anthropic", new String[]{}, "ANTHROPIC_API_KEY", "Anthropic", "", new String[0], false, false, "", "", "", false);
    }

    private static String applyPrefix(String model, ProviderSpec spec) {
        if (spec.litellmPrefix() == null || spec.litellmPrefix().isEmpty()) return model;
        for (String skip : spec.skipPrefixes()) {
            if (model.startsWith(skip)) return model;
        }
        return spec.litellmPrefix() + "/" + model;
    }

    private static String getApiKey(ProviderSpec spec, AppConfig config) {
        String key = config.getApiKeyForProvider(spec.name());
        if (key != null && !key.isEmpty()) return key;
        return System.getenv(spec.envKey() != null ? spec.envKey() : "");
    }

    private static String getApiBase(ProviderSpec spec, AppConfig config) {
        String base = config.getApiBaseForProvider(spec.name());
        if (base != null && !base.isEmpty()) return base;
        return spec.defaultApiBase() != null && !spec.defaultApiBase().isEmpty() ? spec.defaultApiBase() : null;
    }
}