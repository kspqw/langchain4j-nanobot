package com.nanobot.tools;

import java.nio.file.Path;
import java.util.List;
import dev.langchain4j.agent.tool.ToolSpecification;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== Testing Tool Executor ===\n");

        // Test 1: Validate tool names
        System.out.println("Test 1: Tool Names Validation");
        ToolExecutor executor = new ToolExecutor(null, Path.of("."), false);
        List<ToolSpecification> specs = executor.getToolSpecifications();

        System.out.println("Found " + specs.size() + " tools:");
        java.util.regex.Pattern validPattern = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");
        boolean allValid = true;

        int i = 0;
        for (ToolSpecification spec : specs) {
            String name = spec.name();
            boolean valid = validPattern.matcher(name).matches();
            System.out.println("  [" + i + "] " + name + " -> valid: " + valid);
            if (!valid) {
                allValid = false;
            }
            i++;
        }

        System.out.println("\nResult: " + (allValid ? "PASS - All tool names are valid!" : "FAIL - Some tool names are invalid!"));

        // Test 2: Write file
        System.out.println("\nTest 2: Write File");
        try {
            String writeResult = executor.execute("write_file", "{\"path\": \"target/test-output/test.txt\", \"content\": \"Hello World\"}");
            System.out.println("  Write result: " + writeResult);

            java.nio.file.Files.writeString(Path.of("target/test-output/test.txt"), "Hello World".replace("World", "Test"));
            String readResult = executor.execute("read_file", "{\"path\": \"target/test-output/test.txt\"}");
            System.out.println("  Read result: " + readResult);
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }

        // Test 3: Edit file
        System.out.println("\nTest 3: Edit File");
        try {
            java.nio.file.Path editFile = Path.of("target/test-output/edit-test.txt");
            java.nio.file.Files.writeString(editFile, "Hello World");
            String editResult = executor.execute("edit_file", "{\"path\": \"target/test-output/edit-test.txt\", \"oldText\": \"World\", \"newText\": \"Universe\"}");
            System.out.println("  Edit result: " + editResult);

            String content = java.nio.file.Files.readString(editFile);
            System.out.println("  File content after edit: " + content);
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }

        System.out.println("\n=== Tests Complete ===");
    }
}