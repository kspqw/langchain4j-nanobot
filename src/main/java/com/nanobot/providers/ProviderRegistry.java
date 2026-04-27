package com.nanobot.providers;

import java.util.*;

public class ProviderRegistry {
    public static final List<ProviderSpec> PROVIDERS = List.of(
        new ProviderSpec("custom", new String[]{}, "", "Custom", "", new String[0], false, false, "", "", "", false),
        new ProviderSpec("azure_openai", new String[]{"azure", "azure-openai"}, "", "Azure OpenAI", "", new String[0], false, false, "", "", "", false),
        new ProviderSpec("openrouter", new String[]{"openrouter"}, "OPENROUTER_API_KEY", "OpenRouter", "openrouter", new String[0], true, false, "sk-or-", "openrouter", "https://openrouter.ai/api/v1", false),
        new ProviderSpec("aihubmix", new String[]{"aihubmix"}, "OPENAI_API_KEY", "AiHubMix", "openai", new String[0], true, false, "", "aihubmix", "https://api.siliconflow.cn/v1", true),
        new ProviderSpec("siliconflow", new String[]{"siliconflow"}, "OPENAI_API_KEY", "SiliconFlow", "openai", new String[0], true, false, "", "siliconflow", "https://api.siliconflow.cn/v1", false),
        new ProviderSpec("volcengine", new String[]{"volcengine", "volces", "ark"}, "OPENAI_API_KEY", "VolcEngine", "volcengine", new String[0], true, false, "", "volces", "https://ark.cn-beijing.volces.com/api/v3", false),
        new ProviderSpec("anthropic", new String[]{"anthropic", "claude"}, "ANTHROPIC_API_KEY", "Anthropic", "", new String[0], false, false, "", "", "", false),
        new ProviderSpec("openai", new String[]{"openai", "gpt"}, "OPENAI_API_KEY", "OpenAI", "", new String[0], false, false, "", "", "", false),
        new ProviderSpec("deepseek", new String[]{"deepseek"}, "DEEPSEEK_API_KEY", "DeepSeek", "", new String[0], false, false, "", "", "https://api.deepseek.com/v1", false),
        new ProviderSpec("gemini", new String[]{"gemini"}, "GEMINI_API_KEY", "Gemini", "gemini", new String[]{"gemini/"}, false, false, "", "", "", false),
        new ProviderSpec("zhipu", new String[]{"zhipu", "glm", "zai"}, "ZAI_API_KEY", "Zhipu AI", "zai", new String[]{"zhipu/", "zai/", "openrouter/"}, false, false, "", "", "", false),
        new ProviderSpec("dashscope", new String[]{"qwen", "dashscope"}, "DASHSCOPE_API_KEY", "DashScope", "dashscope", new String[]{"dashscope/", "openrouter/"}, false, false, "", "", "", false),
        new ProviderSpec("moonshot", new String[]{"moonshot", "kimi"}, "MOONSHOT_API_KEY", "Moonshot", "moonshot", new String[]{"moonshot/", "openrouter/"}, false, false, "", "", "https://api.moonshot.ai/v1", false),
        new ProviderSpec("minimax", new String[]{"minimax"}, "MINIMAX_API_KEY", "MiniMax", "minimax", new String[]{"minimax/", "openrouter/"}, false, false, "", "", "https://api.minimax.io/v1", false),
        new ProviderSpec("vllm", new String[]{"vllm"}, "HOSTED_VLLM_API_KEY", "vLLM/Local", "hosted_vllm", new String[0], false, true, "", "", "", false),
        new ProviderSpec("groq", new String[]{"groq"}, "GROQ_API_KEY", "Groq", "groq", new String[]{"groq/"}, false, false, "", "", "", false)
    );

    public static ProviderSpec findByModel(String model) {
        String lower = model.toLowerCase();
        String prefix = lower.contains("/") ? lower.split("\\/")[0] : "";
        for (ProviderSpec spec : PROVIDERS) {
            if (!spec.isGateway() && !spec.isLocal()) {
                for (String kw : spec.keywords()) {
                    if (lower.contains(kw) || (prefix.equals(spec.name().replace("_", "-")))) {
                        return spec;
                    }
                }
            }
        }
        return null;
    }

    public static ProviderSpec findGateway(String providerName, String apiKey, String apiBase) {
        if (providerName != null) {
            for (ProviderSpec spec : PROVIDERS) {
                if (spec.name().equals(providerName) && (spec.isGateway() || spec.isLocal())) {
                    return spec;
                }
            }
        }
        for (ProviderSpec spec : PROVIDERS) {
            if (spec.isGateway() || spec.isLocal()) {
                if (spec.detectByKeyPrefix() != null && !spec.detectByKeyPrefix().isEmpty()
                        && apiKey != null && apiKey.startsWith(spec.detectByKeyPrefix())) {
                    return spec;
                }
                if (spec.detectByBaseKeyword() != null && !spec.detectByBaseKeyword().isEmpty()
                        && apiBase != null && apiBase.contains(spec.detectByBaseKeyword())) {
                    return spec;
                }
            }
        }
        return null;
    }

    public static ProviderSpec findByName(String name) {
        for (ProviderSpec spec : PROVIDERS) {
            if (spec.name().equals(name)) return spec;
        }
        return null;
    }
}