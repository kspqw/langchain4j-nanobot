package com.nanobot.tools;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import dev.langchain4j.agent.tool.ToolSpecification;

public class QuickTest {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Tool Executor Test ===\n\n");

        try {
            ToolExecutor executor = new ToolExecutor(null, Path.of("."), false);
            List<ToolSpecification> specs = executor.getToolSpecifications();

            sb.append("Tools found: ").append(specs.size()).append("\n");
            Pattern validPattern = Pattern.compile("^[a-zA-Z0-9_-]+$");
            boolean allValid = true;

            for (int i = 0; i < specs.size(); i++) {
                String name = specs.get(i).name();
                boolean valid = validPattern.matcher(name).matches();
                sb.append("[").append(i).append("] ").append(name).append(" -> ").append(valid ? "OK" : "INVALID").append("\n");
                if (!valid) allValid = false;
            }

            sb.append("\nResult: ").append(allValid ? "ALL VALID" : "SOME INVALID").append("\n\n");

            // Test write
            sb.append("Test write_file:\n");
            Path testDir = Paths.get("target/test-output");
            Files.createDirectories(testDir);
            Path testFile = testDir.resolve("test.txt");
            String writeResult = executor.execute("write_file", "{\"path\": \"target/test-output/test.txt\", \"content\": \"Hello Test\"}");
            sb.append("  Result: ").append(writeResult).append("\n");

            if (Files.exists(testFile)) {
                sb.append("  File exists: OK\n");
            } else {
                sb.append("  File exists: FAILED\n");
            }

        } catch (Exception e) {
            sb.append("Error: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        // Write to file
        try {
            Files.writeString(Paths.get("target/test-output/quick-test-result.txt"), sb.toString());
        } catch (Exception e) {}

        System.out.println(sb.toString());
    }
}