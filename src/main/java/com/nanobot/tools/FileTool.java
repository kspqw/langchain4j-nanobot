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
    public String readFile(@P("Absolute path to the file") String path) {
        try {
            Path p = Path.of(path);
            if (!Files.exists(p)) return "File not found: " + path;
            return Files.readString(p);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool("Write content to a file, creating or overwriting it")
    public String writeFile(@P("Absolute path to the file") String path,
                            @P("Content to write") String content) {
        try {
            Path p = Path.of(path);
            Files.createDirectories(p.getParent());
            Files.writeString(p, content);
            return "File written successfully: " + path;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    @Tool("Edit an existing file by replacing a target string with replacement")
    public String editFile(@P("Absolute path to the file") String path,
                           @P("Text to find") String oldText,
                           @P("Text to replace it with") String newText) {
        try {
            Path p = Path.of(path);
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
    public String listDir(@P("Absolute path to the directory") String path) {
        try {
            Path p = Path.of(path);
            StringBuilder sb = new StringBuilder();
            for (Path f : Files.list(p).toList()) {
                sb.append(f.getFileName());
                sb.append(Files.isDirectory(f) ? "/" : "");
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error listing directory: " + e.getMessage();
        }
    }
}