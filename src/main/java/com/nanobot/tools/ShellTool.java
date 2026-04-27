package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class ShellTool {
    private final Path workingDir;
    private final int timeout;
    private final boolean restrictToWorkspace;
    private final String pathAppend;

    public ShellTool(Path workingDir, int timeout, boolean restrictToWorkspace, String pathAppend) {
        this.workingDir = workingDir;
        this.timeout = timeout;
        this.restrictToWorkspace = restrictToWorkspace;
        this.pathAppend = pathAppend;
    }

    @Tool("Execute a shell command and return its output")
    public String exec(@P("Shell command to execute") String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return "Command timed out after " + timeout + "s";
            }
            return output;
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}