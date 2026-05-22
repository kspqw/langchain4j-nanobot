package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileTool {
    private final Path workspace;
    private final boolean restrictToWorkspace;

    public FileTool(Path workspace, boolean restrictToWorkspace) {
        this.workspace = workspace;
        this.restrictToWorkspace = restrictToWorkspace;
    }

    @Tool("Read the contents of a file")
    public String readFile(@P("path") String path) {
        try {
            Path p = resolvePath(path);
            if (!Files.exists(p)) return "File not found: " + path;
            return Files.readString(p);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool("Write content to a file, creating or overwriting it")
    public String writeFile(@P("path") String path,
                            @P("content") String content) {
        try {
            Path p = resolvePath(path);
            Files.createDirectories(p.getParent());
            Files.writeString(p, content);
            return "File written successfully: " + path;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    // 解析路径，支持 ~ 展开
    private Path resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return workspace;
        }

        // 处理 ~ 路径展开
        if (path.startsWith("~/") || path.equals("~")) {
            String home = System.getProperty("user.home");
            String expanded = path.replace("~", home);
            return Path.of(expanded);
        }

        try {
            // 对于已经是绝对路径的，直接返回
            Path p = Path.of(path);
            if (p.isAbsolute()) {
                return p;
            }
            // 相对路径基于 workspace
            return workspace.resolve(p).normalize();
        } catch (Exception e) {
            // 如果路径解析失败，返回 workspace
            return workspace;
        }
    }

    @Tool("Edit an existing file by replacing a target string with replacement")
    public String editFile(@P("path") String path,
                           @P("oldText") String oldText,
                           @P("newText") String newText) {
        try {
            Path p = resolvePath(path);
            String content = Files.readString(p);
            if (!content.contains(oldText)) return "Text not found in file";
            String updated = content.replace(oldText, newText);
            Files.writeString(p, updated);
            return "File edited successfully";
        } catch (Exception e) {
            return "Error editing file: " + e.getMessage();
        }
    }

    @Tool("List files and directories at a given path")
    public String listDir(@P("path") String path) {
        try {
            Path p = resolvePath(path);
            if (p == null) {
                return "Invalid path: " + path;
            }
            // 如果目录不存在，尝试创建
            if (!Files.exists(p)) {
                Files.createDirectories(p);
            }
            StringBuilder sb = new StringBuilder();
            var entries = Files.list(p).toList();
            // 智能排序：目录在前，文件在后，按名称
            entries.sort((a, b) -> {
                boolean aDir = Files.isDirectory(a);
                boolean bDir = Files.isDirectory(b);
                if (aDir != bDir) return bDir ? 1 : -1;
                return a.getFileName().compareTo(b.getFileName());
            });
            for (Path f : entries) {
                sb.append(f.getFileName());
                sb.append(Files.isDirectory(f) ? "/" : "");
                sb.append("\n");
            }
            sb.append("\n---\nTotal: ").append(entries.size()).append(" items");
            return sb.toString();
        } catch (Exception e) {
            return "Error listing directory: " + e.getMessage();
        }
    }

    // 新的：显示目录树
    @Tool("List directory tree structure recursively")
    public String treeDir(@P("path") String path) {
        try {
            Path p = resolvePath(path);
            if (!Files.exists(p)) return "Path not found: " + path;
            if (Files.isRegularFile(p)) return "Not a directory: " + path;

            StringBuilder sb = new StringBuilder();
            buildTree(p, "", sb);
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void buildTree(Path dir, String prefix, StringBuilder sb) {
        try {
            var entries = Files.list(dir).toList();
            entries.sort((a, b) -> {
                boolean aDir = Files.isDirectory(a);
                boolean bDir = Files.isDirectory(b);
                if (aDir != bDir) return bDir ? 1 : -1;
                return a.getFileName().compareTo(b.getFileName());
            });
            for (int i = 0; i < entries.size(); i++) {
                Path f = entries.get(i);
                boolean isDir = Files.isDirectory(f);
                boolean last = i == entries.size() - 1;
                sb.append(prefix).append(last ? "└── " : "├── ");
                sb.append(f.getFileName());
                sb.append(isDir ? "/" : "").append("\n");
                if (isDir) {
                    buildTree(f, prefix + (last ? "    " : "│   "), sb);
                }
            }
        } catch (Exception e) {}
    }
}
