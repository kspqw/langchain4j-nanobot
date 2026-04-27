package com.nanobot.agent.memory;

import com.nanobot.session.Session;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class MemoryStore {
    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    private final Path memoryDir;
    private final Path memoryFile;
    private final Path historyFile;

    public MemoryStore(Path workspace) {
        this.memoryDir = ensureDir(workspace.resolve("memory"));
        this.memoryFile = memoryDir.resolve("MEMORY.md");
        this.historyFile = memoryDir.resolve("HISTORY.md");
    }

    private Path ensureDir(Path dir) {
        try { Files.createDirectories(dir); } catch (Exception e) {}
        return dir;
    }

    public String readLongTerm() {
        if (Files.exists(memoryFile)) {
            try { return Files.readString(memoryFile); } catch (Exception e) {}
        }
        return "";
    }

    public void writeLongTerm(String content) {
        try { Files.writeString(memoryFile, content); } catch (Exception e) {}
    }

    public void appendHistory(String entry) {
        try { Files.writeString(historyFile, entry + "\n\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception e) {}
    }

    public String getMemoryContext() {
        String lt = readLongTerm();
        return lt.isEmpty() ? "" : "## Long-term Memory\n" + lt;
    }

    public boolean consolidate(Session session, ChatModel model, String modelName,
                               boolean archiveAll, int memoryWindow) {
        int keepCount = archiveAll ? 0 : memoryWindow / 2;
        if (!archiveAll && session.messages.size() <= keepCount) return true;

        List<Map<String, Object>> oldMessages;
        if (archiveAll) {
            oldMessages = new ArrayList<>(session.messages);
        } else {
            int start = Math.max(0, session.lastConsolidated);
            int end = Math.max(0, session.messages.size() - keepCount);
            oldMessages = new ArrayList<>(session.messages.subList(start, end));
        }
        if (oldMessages.isEmpty()) return true;

        StringBuilder lines = new StringBuilder();
        for (Map<String, Object> m : oldMessages) {
            Object content = m.get("content");
            if (content == null) continue;
            String ts = m.get("timestamp") != null ? m.get("timestamp").toString().substring(0, 16) : "?";
            String role = ((String) m.get("role")).toUpperCase();
            lines.append("[").append(ts).append("] ").append(role).append(": ").append(content).append("\n");
        }

        String currentMemory = readLongTerm();
        String prompt = "Process this conversation and consolidate important information.\n\n" +
            "## Current Long-term Memory\n" + (currentMemory.isEmpty() ? "(empty)" : currentMemory) + "\n\n" +
            "## Conversation to Process\n" + lines;

        try {
            List<ChatMessage> msgs = List.of(
                SystemMessage.from("You are a memory consolidation agent. Summarize key information."),
                UserMessage.from(prompt)
            );

            String response = model.chat(msgs).aiMessage().text().trim();
            if (!currentMemory.isEmpty() && !response.equals(currentMemory)) {
                writeLongTerm(response);
            }

            session.lastConsolidated = archiveAll ? 0 : session.messages.size() - keepCount;
            logger.info("Memory consolidation done: lastConsolidated={}", session.lastConsolidated);
            return true;
        } catch (Exception e) {
            logger.error("Memory consolidation failed", e);
            return false;
        }
    }
}