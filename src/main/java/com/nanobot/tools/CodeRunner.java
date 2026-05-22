package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class CodeRunner {
    private static final int TIMEOUT = 30;
    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "nanobot-code");

    static {
        try { Files.createDirectories(TEMP_DIR); } catch (Exception e) {}
    }

    @Tool("Run Python or JavaScript code and return the output")
    public String runCode(@P("code") String code, @P("language") String language) {
        if (code == null || code.isBlank()) {
            return "Error: Empty code";
        }

        String lang = (language != null ? language : detectLanguage(code)).toLowerCase();
        String output;

        try {
            if (lang.contains("python") || lang.contains("py")) {
                output = runPython(code);
            } else if (lang.contains("javascript") || lang.contains("js") || lang.contains("node")) {
                output = runJS(code);
            } else if (lang.contains("java")) {
                output = runJava(code);
            } else {
                return "Unsupported language: " + lang + "\nSupported: python, javascript, java";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        return output;
    }

    // 自动检测语言
    private String detectLanguage(String code) {
        if (code.contains("def ") || code.contains("import ") || code.contains("print(")) {
            return "python";
        }
        if (code.contains("console.log") || code.contains("const ") || code.contains("function ") || code.contains("=>")) {
            return "javascript";
        }
        if (code.contains("public class") || code.contains("System.out")) {
            return "java";
        }
        return "python"; // 默认
    }

    private String runPython(String code) throws Exception {
        // 检查是否有 Python
        ProcessBuilder checkPb = new ProcessBuilder("python", "--version");
        checkPb.redirectErrorStream(true);
        Process checkProcess = checkPb.start();
        if (checkProcess.waitFor(2, TimeUnit.SECONDS) && checkProcess.exitValue() != 0) {
            return "Python not found. Please install Python or specify another language.";
        }

        Path tempFile = TEMP_DIR.resolve("temp_" + System.currentTimeMillis() + ".py");
        try {
            Files.writeString(tempFile, code, StandardOpenOption.CREATE);

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "python " + tempFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            if (!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "⏱️ Timed out after " + TIMEOUT + "s";
            }

            String output = new String(p.getInputStream().readAllBytes());
            return p.exitValue() == 0 ? output : "Error (exit " + p.exitValue() + "):\n" + output;
        } finally {
            tryDelete(tempFile);
        }
    }

    private String runJS(String code) throws Exception {
        // 检查 Node.js
        ProcessBuilder checkPb = new ProcessBuilder("node", "--version");
        checkPb.redirectErrorStream(true);
        Process checkProcess = checkPb.start();
        if (checkProcess.waitFor(2, TimeUnit.SECONDS) && checkProcess.exitValue() != 0) {
            return "Node.js not found. Please install Node.js or specify another language.";
        }

        Path tempFile = TEMP_DIR.resolve("temp_" + System.currentTimeMillis() + ".js");
        try {
            Files.writeString(tempFile, code, StandardOpenOption.CREATE);

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "node " + tempFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            if (!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "⏱️ Timed out after " + TIMEOUT + "s";
            }

            String output = new String(p.getInputStream().readAllBytes());
            return p.exitValue() == 0 ? output : "Error (exit " + p.exitValue() + "):\n" + output;
        } finally {
            tryDelete(tempFile);
        }
    }

    private String runJava(String code) throws Exception {
        // 简化版：只支持单文件简单类
        Pattern classPat = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher m = classPat.matcher(code);
        if (!m.find()) {
            return "Java code must have a public class";
        }
        String className = m.group(1);

        Path tempFile = TEMP_DIR.resolve(className + ".java");
        Path classFile = TEMP_DIR.resolve(className + ".class");
        try {
            Files.writeString(tempFile, code, StandardOpenOption.CREATE);

            // 编译
            ProcessBuilder compilePb = new ProcessBuilder("cmd", "/c", "javac " + tempFile);
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            if (!compileProcess.waitFor(10, TimeUnit.SECONDS) || compileProcess.exitValue() != 0) {
                String err = new String(compileProcess.getInputStream().readAllBytes());
                return "Compilation error:\n" + err;
            }

            // 运行
            ProcessBuilder runPb = new ProcessBuilder("cmd", "/c", "java -cp " + TEMP_DIR + " " + className);
            runPb.redirectErrorStream(true);
            Process p = runPb.start();

            if (!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "⏱️ Timed out after " + TIMEOUT + "s";
            }

            String output = new String(p.getInputStream().readAllBytes());
            return p.exitValue() == 0 ? output : "Error (exit " + p.exitValue() + "):\n" + output;
        } finally {
            tryDelete(tempFile);
            tryDelete(classFile);
        }
    }

    private void tryDelete(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception e) {}
    }
}