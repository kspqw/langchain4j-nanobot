package com.nanobot.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nanobot.bus.MessageBus;

import dev.langchain4j.agent.tool.*;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolExecutor {
    private final Map<String, ToolMethod> tools = new HashMap<>();
    private static final Gson gson = new Gson();

    // 需要确认的工具
    private static final Set<String> DANGEROUS_TOOLS = Set.of(
        "execute_command", "execute_command_with_confirm",
        "write_file", "edit_file", "send_message"
    );

    public ToolExecutor(MessageBus bus, Path workspace, boolean restrictToWorkspace) {
        registerTool("read_file", new FileTool(workspace, restrictToWorkspace));
        registerTool("write_file", new FileTool(workspace, restrictToWorkspace));
        registerTool("edit_file", new FileTool(workspace, restrictToWorkspace));
        registerTool("list_dir", new FileTool(workspace, restrictToWorkspace));
        registerTool("tree_dir", new FileTool(workspace, restrictToWorkspace));
        registerTool("execute_command", new ShellTool(workspace, 60, restrictToWorkspace, ""));
        registerTool("execute_command_with_confirm", new ShellTool(workspace, 60, restrictToWorkspace, ""));
        registerTool("visit_webpage", new WebTool(null, null));
        registerTool("web_search", new WebTool(null, null));
        registerTool("send_message", new MessageTool(bus));
        registerTool("schedule", new CronTool());
        registerTool("run_code", new CodeRunner());
        System.out.println("[ToolExecutor] Registered tools: " + tools.keySet());
        System.out.println("[ToolExecutor] Total tools: " + tools.size());
    }

    private void registerTool(String name, Object tool) {
        for (Method m : tool.getClass().getMethods()) {
            Tool annotation = m.getAnnotation(Tool.class);
            if (annotation != null) {
                // 1. 优先使用方法名作为工具名，因为它通常是合法的标识符
                String toolName = m.getName();

                // 2. 如果方法名是通用的（如 exec, call），则尝试从注解中提取简短名称
                // 注意：不要直接使用 annotation.value()[0] 作为工具名，因为它通常包含空格或描述性文字
                if ("exec".equals(toolName) || "call".equals(toolName)) {
                    // 这里可以根据具体的工具类逻辑进行映射，或者保持使用方法名
                    // 例如 ShellTool 的 exec 方法，我们可以手动指定为 execute_command
                    if (tool instanceof ShellTool) {
                        toolName = "execute_command";
                    } else if (tool instanceof WebTool && m.getName().equals("webSearch")) {
                        toolName = "web_search";
                    } else if (tool instanceof WebTool && m.getName().equals("visitWebpage")) {
                        toolName = "visit_webpage";
                    }
                }

                // 3. 强制清理工具名，确保符合 ^[a-zA-Z0-9_-]+$ 规范
                toolName = sanitizeToolName(toolName);

                tools.put(toolName, new ToolMethod(tool, m));
            }
        }
    }

    /**
     * 清理工具名称，确保只包含字母、数字、下划线和连字符
     */
    private String sanitizeToolName(String name) {
        if (name == null) return "unknown_tool";
        // 替换所有非字母数字、非下划线、非连字符的字符为下划线
        String sanitized = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        // 避免以数字或连字符开头（某些 API 可能有限制）
        if (sanitized.matches("^[0-9-].*")) {
            sanitized = "tool_" + sanitized;
        }
        return sanitized;
    }

    public String execute(String name, String argumentsJson) {
        return execute(name, argumentsJson, false);
    }

    public String execute(String name, String argumentsJson, boolean confirmed) {
        ToolMethod tm = tools.get(name);
        if (tm == null) {
            System.out.println("[ToolExecutor] Tool NOT FOUND: " + name + " Available tools: " + tools.keySet());
            return "Tool not found: " + name;
        }

        // 危险工具需要确认
        if (DANGEROUS_TOOLS.contains(name) && !confirmed) {
            String args = parseArgs(argumentsJson).toString();
            String warning = buildWarningMessage(name, args);
            // 检查是否包含确认关键词
            if (!args.contains("CONFIRM") && !args.contains("确认执行")) {
                System.out.println("[ToolExecutor] Dangerous tool needs confirmation: " + name);
                return warning + "\n\n如果要确认执行危险操作，请使用 `execute_command_with_confirm` 工具或回复 `确认执行`";
            }
        }

        System.out.println("[ToolExecutor] Executing tool: " + name + " with args: " + argumentsJson);

        try {
            Map<String, String> args = parseArgs(argumentsJson);
            System.out.println("[ToolExecutor] Parsed args: " + args);

            Object[] paramValues = new Object[tm.paramTypes.length];
            for (int i = 0; i < tm.paramTypes.length; i++) {
                String argName = tm.paramNames[i];
                String value = args.get(argName);

                // 如果找不到，尝试映射后的参数名
                if (value == null || value.isEmpty()) {
                    value = args.get(reverseMapParamName(argName));
                }
                // 再尝试原始名小写
                if (value == null || value.isEmpty()) {
                    value = args.get(argName.toLowerCase());
                }
                // 最后尝试任何包含此参数名的key
                if (value == null || value.isEmpty()) {
                    for (var entry : args.entrySet()) {
                        if (entry.getKey().toLowerCase().contains(argName.toLowerCase())) {
                            value = entry.getValue();
                            break;
                        }
                    }
                }

                System.out.println("[ToolExecutor] Param " + argName + " = " + value);

                if (value == null || value.isEmpty()) {
                    return "Missing required parameter: " + argName;
                }

                paramValues[i] = convertValue(value, tm.paramTypes[i]);
            }

            Object result = tm.method.invoke(tm.toolInstance, paramValues);
            String resultStr = result != null ? result.toString() : "OK";

            if (resultStr == null || resultStr.isBlank()) {
                resultStr = "(tool executed successfully with no output)";
            }

            System.out.println("[ToolExecutor] Tool result: " + resultStr);
            return resultStr;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing tool: " + e.getMessage();
        }
    }

    private Map<String, String> parseArgs(String json) {
        Map<String, String> args = new HashMap<>();

        if (json == null || json.isBlank()) {
            return args;
        }

        try {
            // 首先尝试直接解析
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
                String key = mapParamName(entry.getKey());
                String value = entry.getValue().getAsString();
                args.put(key, value);
            }
        } catch (Exception e) {
            // 如果失败，尝试修复 JSON 后再次解析
            System.out.println("[ToolExecutor] Direct Gson parsing failed, trying to fix JSON: " + e.getMessage());
            try {
                String fixedJson = fixMalformedJson(json);
                JsonObject jsonObject = JsonParser.parseString(fixedJson).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
                    String key = mapParamName(entry.getKey());
                    String value = entry.getValue().getAsString();
                    args.put(key, value);
                }
            } catch (Exception ex) {
                System.out.println("[ToolExecutor] Fixed JSON parsing also failed: " + ex.getMessage());
            }
        }

        System.out.println("[ToolExecutor] Parsed args: " + args);
        return args;
    }

    /**
     * 修复 AI 生成的不规范 JSON
     * 主要处理键名缺少闭合引号的情况: "key: "value" -> "key": "value"
     */
    private String fixMalformedJson(String json) {
        // 匹配模式: "key: "value" 或 "key: value
        // 将 "SomeKey: 替换为 "SomeKey":
        Pattern pattern = Pattern.compile("\"([^\"]+?):\\s*\"");
        Matcher matcher = pattern.matcher(json);
        String fixed = matcher.replaceAll("\"$1\": \"");

        // 也处理没有值引号的情况: "key: value" -> "key": "value"
        // 这个比较复杂，暂时先处理最常见的情况

        return fixed;
    }

    private String mapParamName(String name) {
        if (name == null) return name;
        // 标准化参数名，匹配工具方法中 @P 注解定义的名称
        return switch (name) {
            case "Absolute", "AbsolutePath", "filepath", "filePath", "FilePath", "path" -> "path";
            case "old_string", "OldText", "target", "oldText" -> "oldText";
            case "New", "NewText", "new_string", "Replacement", "newText" -> "newText";
            case "Content", "text", "Text", "body", "content" -> "content";
            case "Url", "URL", "link", "url" -> "url";
            case "Message", "msg", "message" -> "message";
            case "Shell", "Command", "cmd", "command" -> "command";
            case "Search", "search_query", "SearchQuery", "searchQuery", "q", "query" -> "query";
            default -> name;
        };
    }

    // 反向映射：工具参数名 -> 模型可能使用的参数名
    private String reverseMapParamName(String toolParamName) {
        if (toolParamName == null) return toolParamName;
        return switch (toolParamName) {
            case "query" -> "Search";
            case "path" -> "Absolute";
            case "oldText" -> "oldText";
            case "newText" -> "newText";
            case "content" -> "content";
            case "url" -> "url";
            case "message" -> "message";
            case "command" -> "command";
            default -> toolParamName;
        };
    }

    // 构建危险操作警告消息
    private String buildWarningMessage(String toolName, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ 警告：这是一个需要确认的操作\n\n");
        sb.append("工具: ").append(toolName).append("\n");
        sb.append("参数: ").append(args).append("\n\n");
        sb.append("请确认是否要执行此操作。\n");
        return sb.toString();
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

    public List<ToolSpecification> getToolSpecifications() {
        Map<String, ToolSpecification> specMap = new LinkedHashMap<>();
        int specIndex = 0;
        for (Map.Entry<String, ToolMethod> entry : tools.entrySet()) {
            ToolMethod tm = entry.getValue();
            String toolName = entry.getKey();
            System.out.println("[ToolExecutor] Building spec " + specIndex + ": " + toolName);

            if (!toolName.matches("^[a-zA-Z0-9_-]+$")) {
                System.out.println("[ToolExecutor] WARNING: Invalid tool name: " + toolName);
            }

            if (specMap.containsKey(toolName)) {
                System.out.println("[ToolExecutor] WARNING: Duplicate tool name: " + toolName);
            }

            JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();

            for (int i = 0; i < tm.paramTypes.length; i++) {
                String paramName = tm.paramNames[i];
                String paramType = tm.paramTypes[i].getSimpleName().toLowerCase();

                if ("string".equals(paramType)) {
                    schemaBuilder.addStringProperty(paramName, "Parameter: " + paramName);
                } else if ("integer".equals(paramType) || "int".equals(paramType)) {
                    schemaBuilder.addIntegerProperty(paramName, "Parameter: " + paramName);
                } else if ("boolean".equals(paramType)) {
                    schemaBuilder.addBooleanProperty(paramName, "Parameter: " + paramName);
                } else if ("number".equals(paramType) || "double".equals(paramType) || "float".equals(paramType)) {
                    schemaBuilder.addNumberProperty(paramName, "Parameter: " + paramName);
                } else {
                    schemaBuilder.addStringProperty(paramName, "Parameter: " + paramName);
                }
            }

            ToolSpecification specification = ToolSpecification.builder()
                    .name(toolName)
                    .description(tm.method.getAnnotation(Tool.class).value()[0])
                    .parameters(schemaBuilder.build())
                    .build();

            specMap.put(toolName, specification);
            specIndex++;
        }
        return new ArrayList<>(specMap.values());
    }
}
