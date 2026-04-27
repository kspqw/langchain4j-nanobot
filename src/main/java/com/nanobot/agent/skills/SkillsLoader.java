package com.nanobot.agent.skills;

import java.nio.file.*;
import java.util.*;

public class SkillsLoader {
    private final Path workspace;
    private final Map<String, Map<String, String>> skillCache = new HashMap<>();

    public SkillsLoader(Path workspace) {
        this.workspace = workspace;
        loadSkillMetadata();
    }

    private void loadSkillMetadata() {
        Path skillsDir = workspace.resolve("skills");
        if (!Files.exists(skillsDir)) return;
        try {
            for (Path entry : Files.list(skillsDir).toList()) {
                if (!Files.isDirectory(entry)) continue;
                Path skillMd = entry.resolve("SKILL.md");
                if (!Files.exists(skillMd)) continue;
                String name = entry.getFileName().toString();
                skillCache.put(name, Map.of("path", skillMd.toString()));
            }
        } catch (Exception e) {}
    }

    public List<String> getAlwaysSkills() {
        return List.of();
    }

    public String loadSkillsForContext(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            Path f = workspace.resolve("skills").resolve(name).resolve("SKILL.md");
            if (Files.exists(f)) {
                try { sb.append(Files.readString(f)).append("\n\n"); } catch (Exception e) {}
            }
        }
        return sb.toString();
    }

    public String buildSkillsSummary() {
        StringBuilder sb = new StringBuilder();
        for (String name : skillCache.keySet()) {
            sb.append("- `").append(name).append("`\n");
        }
        return sb.toString();
    }
}