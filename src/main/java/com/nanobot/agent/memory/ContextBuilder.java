package com.nanobot.agent.memory;

import com.nanobot.agent.skills.SkillsLoader;
import com.nanobot.session.Session;
import dev.langchain4j.data.message.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ContextBuilder {
    private static final String[] BOOTSTRAP_FILES = {"AGENTS.md", "SOUL.md", "USER.md", "TOOLS.md", "IDENTITY.md"};
    private static final String RUNTIME_CONTEXT_TAG = "[Runtime Context]";

    private final Path workspace;
    private final MemoryStore memory;
    private final SkillsLoader skills;

    public ContextBuilder(Path workspace) {
        this.workspace = workspace;
        this.memory = new MemoryStore(workspace);
        this.skills = new SkillsLoader(workspace);
    }

    public String buildSystemPrompt(List<String> skillNames) {
        List<String> parts = new ArrayList<>();
        parts.add(getIdentity());

        String bootstrap = loadBootstrapFiles();
        if (!bootstrap.isEmpty()) parts.add(bootstrap);

        String memCtx = memory.getMemoryContext();
        if (!memCtx.isEmpty()) parts.add("# Memory\n\n" + memCtx);

        List<String> always = skills.getAlwaysSkills();
        if (!always.isEmpty()) {
            String alwaysContent = skills.loadSkillsForContext(always);
            if (!alwaysContent.isEmpty()) parts.add("# Active Skills\n\n" + alwaysContent);
        }

        String summary = skills.buildSkillsSummary();
        if (!summary.isEmpty()) {
            parts.add("# Skills\n\n" + summary);
        }

        return String.join("\n\n---\n\n", parts);
    }

    private String getIdentity() {
        String runtime = System.getProperty("os.name") + " " + System.getProperty("os.arch") + ", Java " + System.getProperty("java.version");
        return "# nanobot\n\nYou are nanobot, a helpful AI assistant.\n\n## Runtime\n" + runtime + "\n\n## Workspace\nYour workspace is at: " + workspace + "\n- Long-term memory: " + workspace + "/memory/MEMORY.md\n- History log: " + workspace + "/memory/HISTORY.md\n- Custom skills: " + workspace + "/skills/{skill-name}/SKILL.md\n\n## nanobot Guidelines\n- State intent before tool calls, but NEVER predict results before receiving them.\n- Before modifying a file, read it first.\n- If a tool call fails, analyze the error before retrying.\n- Ask for clarification when the request is ambiguous.\n\nReply directly with text for conversations. Only use the 'sendMessage' tool to send to a specific chat channel.";
    }

    private String loadBootstrapFiles() {
        StringBuilder sb = new StringBuilder();
        for (String fname : BOOTSTRAP_FILES) {
            Path f = workspace.resolve(fname);
            if (Files.exists(f)) {
                try {
                    String content = Files.readString(f);
                    sb.append("## ").append(fname).append("\n\n").append(content).append("\n\n");
                } catch (Exception e) {}
            }
        }
        return sb.toString().trim();
    }

    public String buildRuntimeContext(String channel, String chatId) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (EEEE)"));
        StringBuilder sb = new StringBuilder(RUNTIME_CONTEXT_TAG + "\nCurrent Time: " + now);
        if (channel != null && chatId != null) {
            sb.append("\nChannel: ").append(channel).append("\nChat ID: ").append(chatId);
        }
        return sb.toString();
    }

    public List<ChatMessage> buildMessages(
            List<Map<String, Object>> history,
            String currentMessage,
            String channel,
            String chatId) {

        String systemPrompt = buildSystemPrompt(null);
        String runtimeCtx = buildRuntimeContext(channel, chatId);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        for (Map<String, Object> m : history) {
            String role = (String) m.get("role");
            Object content = m.get("content");
            if ("user".equals(role)) {
                messages.add(UserMessage.from(runtimeCtx + "\n\n" + content));
            } else if ("assistant".equals(role)) {
                messages.add(AiMessage.from(content != null ? content.toString() : ""));
            }
        }

        messages.add(UserMessage.from(runtimeCtx + "\n\n" + currentMessage));
        return messages;
    }
}