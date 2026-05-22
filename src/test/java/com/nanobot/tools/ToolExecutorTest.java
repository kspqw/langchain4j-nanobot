package com.nanobot.tools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import dev.langchain4j.agent.tool.ToolSpecification;

public class ToolExecutorTest {

    @Test
    public void testToolNamesAreValid() {
        // Create a minimal tool executor (using null for message bus)
        ToolExecutor executor = new ToolExecutor(null, Path.of("."), false);

        // Get all tool specifications
        List<ToolSpecification> specs = executor.getToolSpecifications();

        System.out.println("=== Tool Specifications ===");
        int i = 0;
        for (ToolSpecification spec : specs) {
            System.out.println("Tool " + i + ": " + spec.name());
            i++;
        }

        // Validate all tool names match the pattern required by DeepSeek
        java.util.regex.Pattern validPattern = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");

        for (ToolSpecification spec : specs) {
            String name = spec.name();
            boolean valid = validPattern.matcher(name).matches();
            System.out.println("  " + name + " -> valid: " + valid);
            assertTrue(valid, "Tool name '" + name + "' does not match pattern ^[a-zA-Z0-9_-]+$");
        }

        System.out.println("All " + specs.size() + " tool names are valid!");
    }

    @Test
    public void testExecuteWriteFile() {
        ToolExecutor executor = new ToolExecutor(null, Path.of("target/test-output"), false);

        // Test writing a file
        String result = executor.execute("write_file", "{\"path\": \"target/test-output/test.txt\", \"content\": \"Hello World\"}");

        System.out.println("Write result: " + result);
        assertTrue(result.contains("success"), "Expected success, got: " + result);

        // Verify file was created
        java.nio.file.Path testFile = Path.of("target/test-output/test.txt");
        assertTrue(java.nio.file.Files.exists(testFile), "File should exist");

        String content = new String(java.nio.file.Files.readAllBytes(testFile));
        assertEquals("Hello World", content);

        System.out.println("File content: " + content);
    }

    @Test
    public void testExecuteReadFile() {
        // First create a test file
        java.nio.file.Path testFile = Path.of("target/test-output/read-test.txt");
        java.nio.file.Files.createDirectories(testFile.getParent());
        java.nio.file.Files.writeString(testFile, "Test Content");

        ToolExecutor executor = new ToolExecutor(null, Path.of("target/test-output"), false);

        // Test reading the file
        String result = executor.execute("read_file", "{\"path\": \"target/test-output/read-test.txt\"}");

        System.out.println("Read result: " + result);
        assertTrue(result.contains("Test Content"), "Expected content, got: " + result);
    }

    @Test
    public void testExecuteListDir() {
        java.nio.file.Files.createDirectories(Path.of("target/test-output/list-test"));
        java.nio.file.Files.writeString(Path.of("target/test-output/list-test/file1.txt"), "content1");
        java.nio.file.Files.writeString(Path.of("target/test-output/list-test/file2.txt"), "content2");

        ToolExecutor executor = new ToolExecutor(null, Path.of("target/test-output"), false);

        // Test listing directory
        String result = executor.execute("list_dir", "{\"path\": \"target/test-output/list-test\"}");

        System.out.println("List result: " + result);
        assertTrue(result.contains("file1.txt"), "Expected file1.txt, got: " + result);
        assertTrue(result.contains("file2.txt"), "Expected file2.txt, got: " + result);
    }

    @Test
    public void testExecuteEditFile() {
        // First create a test file
        java.nio.file.Path testFile = Path.of("target/test-output/edit-test.txt");
        java.nio.file.Files.writeString(testFile, "Hello World");

        ToolExecutor executor = new ToolExecutor(null, Path.of("."), false);

        // Test editing the file
        String result = executor.execute("edit_file", "{\"path\": \"target/test-output/edit-test.txt\", \"oldText\": \"World\", \"newText\": \"Universe\"}");

        System.out.println("Edit result: " + result);

        // Verify file was edited
        String content = java.nio.file.Files.readString(testFile);
        assertEquals("Hello Universe", content);

        System.out.println("Edited content: " + content);
    }
}