package com.nanobot.tools;

import com.nanobot.bus.MessageBus;
import com.nanobot.tools.FileTool;
import com.nanobot.tools.ShellTool;
import com.nanobot.tools.WebTool;
import com.nanobot.tools.MessageTool;
import com.nanobot.tools.CronTool;
import dev.langchain4j.agent.tool.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

public class ToolExecutor {
    private final Map<String, ToolMethod> tools = new HashMap<>();

    public ToolExecutor(MessageBus bus, Path workspace, boolean restrictToWorkspace) {
        registerTool("read_file", new FileTool(workspace, restrictToWorkspace));
        registerTool("write_file", new FileTool(workspace, restrictToWorkspace));
        registerTool("edit_file", new FileTool(workspace, restrictToWorkspace));
        registerTool("list_dir", new FileTool(workspace, restrictToWorkspace));
        registerTool("executeCommand", new ShellTool(workspace, 60, restrictToWorkspace, ""));
        registerTool("visit_webpage", new WebTool(null, null));
        registerTool("sendMessage", new MessageTool(bus));
        registerTool("schedule", new CronTool());
    }

    private void registerTool(String name, Object tool) {
        for (Method m : tool.getClass().getMethods()) {
            Tool annotation = m.getAnnotation(Tool.class);
            if (annotation != null) {
                // 获取 @Tool 注解的值
                String[] toolValues = annotation.value();
                String toolName = (toolValues.length > 0 && !toolValues[0].isEmpty()) ? toolValues[0] : m.getName();
                // 映射常见工具名
                if (toolName.equals("Read the contents of a file")) toolName = "read_file";
                else if (toolName.equals("Write content to a file")) toolName = "write_file";
                else if (toolName.equals("Edit an existing file")) toolName = "edit_file";
                else if (toolName.equals("List files and directories")) toolName = "list_dir";
                else if (toolName.equals("Execute a shell command")) toolName = "executeCommand";
                else if (toolName.equals("Fetch webpage")) toolName = "visit_webpage";
                else if (toolName.equals("Send a message to channels")) toolName = "sendMessage";
                else if (toolName.equals("Schedule a task")) toolName = "schedule";
                // Also map method names with underscores
                else if (m.getName().equals("readFile")) toolName = "read_file";
                else if (m.getName().equals("writeFile")) toolName = "write_file";
                else if (m.getName().equals("editFile")) toolName = "edit_file";
                else if (m.getName().equals("listDir")) toolName = "list_dir";
                else if (m.getName().equals("executeCommand")) toolName = "executeCommand";
                else if (m.getName().equals("visitWebpage")) toolName = "visit_webpage";
                else if (m.getName().equals("sendMessage")) toolName = "sendMessage";
                else if (m.getName().equals("schedule")) toolName = "schedule";
                tools.put(toolName, new ToolMethod(tool, m));
            }
        }
    }

    public String execute(String name, String argumentsJson) {
        ToolMethod tm = tools.get(name);
        if (tm == null) {
            return "Tool not found: " + name;
        }

        try {
            // 简单解析 JSON 参数
            Map<String, String> args = parseArgs(argumentsJson);
            Object[] paramValues = new Object[args.size()];
            int i = 0;
            for (Class<?> paramType : tm.paramTypes) {
                String argName = tm.paramNames[i];
                String value = args.get(argName);
                if (value == null) {
                    value = "";
                }
                paramValues[i] = convertValue(value, paramType);
                i++;
            }
            Object result = tm.method.invoke(tm.toolInstance, paramValues);
            return result != null ? result.toString() : "OK";
        } catch (Exception e) {
            return "Error executing tool: " + e.getMessage();
        }
    }

    private Map<String, String> parseArgs(String json) {
        Map<String, String> args = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        for (String pair : json.split(",")) {
            int colon = pair.indexOf(':');
            if (colon > 0) {
                String key = pair.substring(0, colon).trim().replaceAll("[\"\\s]", "");
                String val = pair.substring(colon + 1).trim();
                if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1);
                }
                args.put(key, val);
            }
        }
        return args;
    }

    private Object convertValue(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) {
            try { return Integer.parseInt(value); } catch (Exception e) { return 0; }
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private static class ToolMethod {
        final Object toolInstance;
        final Method method;
        final Class<?>[] paramTypes;
        final String[] paramNames;

        ToolMethod(Object toolInstance, Method method) {
            this.toolInstance = toolInstance;
            this.method = method;
            this.paramTypes = method.getParameterTypes();
            this.paramNames = Arrays.stream(method.getParameters())
                    .map(p -> p.getAnnotation(P.class).value())
                    .toArray(String[]::new);
        }
    }

    public Collection<Object> getAllTools() {
        Set<Object> result = new HashSet<>();
        for (ToolMethod tm : tools.values()) {
            result.add(tm.toolInstance);
        }
        return result;
    }
}