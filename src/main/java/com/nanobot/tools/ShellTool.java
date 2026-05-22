package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ShellTool {
    private final Path workingDir;
    private final int timeout;
    private final boolean restrictToWorkspace;
    private final String pathAppend;

    // 危险命令模式
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
        "del", "rm", "rmdir", "format", "chkdsk", "diskpart",
        "shutdown", "restart", "taskkill", "reg delete",
        "netsh", "bcdedit", "sysprep"
    );

    // 需要确认的命令模式
    private static final Set<String> CONFIRM_COMMANDS = Set.of(
        "del ", "rm ", "rmdir ", "format ", "mv ", "move ",
        "echo ", "curl ", "wget ", "powershell", "python", "node"
    );

    public ShellTool(Path workingDir, int timeout, boolean restrictToWorkspace, String pathAppend) {
        this.workingDir = workingDir;
        this.timeout = timeout;
        this.restrictToWorkspace = restrictToWorkspace;
        this.pathAppend = pathAppend;
    }

    @Tool("Execute a shell command in Windows CMD environment. Use 'findstr' instead of 'grep', and avoid Linux-only commands like 'head' or 'tail'.")
    public String exec(@P("command") String command) {
        return execInternal(command, false);
    }

    // 内部执行（带确认标志）
    public String execWithConfirm(@P("command") String command) {
        return execInternal(command, true);
    }

    private String execInternal(String command, boolean confirmed) {
        if (command == null || command.isBlank()) {
            return "Error: Empty command provided";
        }

        // 危险命令检测
        String dangerCheck = checkDanger(command);
        if (dangerCheck != null && !confirmed) {
            return "⚠️ 确认 needed: " + dangerCheck +
                   "\n如果确认要执行，请回复 `确认执行` 或工具名改为 `execute_command_with_confirm`";
        }

        // 完全禁止的命令
        if (isBlocked(command)) {
            return "⛔ Blocked: Dangerous command - " + extractCmd(command);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();

            String output = new String(p.getInputStream().readAllBytes());
            boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                p.destroyForcibly();
                return "⏱️ Timed out after " + timeout + "s";
            }

            int exitCode = p.exitValue();
            if (output.isBlank()) {
                return exitCode == 0 ? "✓ Done (exit 0)" : "⚠️ Exit code: " + exitCode;
            }

            return output.length() > 4000 ? output.substring(0, 4000) + "\n... (truncated)" : output.trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String checkDanger(String cmd) {
        String lower = cmd.toLowerCase();
        for (String c : CONFIRM_COMMANDS) {
            if (lower.startsWith(c) || lower.contains(" " + c)) {
                return "命令可能危险: " + extractCmd(cmd);
            }
        }
        return null;
    }

    private boolean isBlocked(String cmd) {
        String lower = cmd.toLowerCase();
        for (String c : DANGEROUS_COMMANDS) {
            if (lower.startsWith(c + " ") || lower.equals(c)) {
                return true;
            }
        }
        // 检查管道危险命令
        if (lower.contains(" | del ") || lower.contains(" | reg ")) {
            return true;
        }
        return false;
    }

    private String extractCmd(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : cmd;
    }

    // 检查命令是否需要确认
    public boolean needsConfirm(String command) {
        return checkDanger(command) != null;
    }
}