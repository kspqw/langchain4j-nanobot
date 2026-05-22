package com.nanobot.agent;

import com.nanobot.agent.memory.*;
import com.nanobot.agent.skills.SkillsLoader;
import com.nanobot.bus.*;
import com.nanobot.config.AppConfig;
import com.nanobot.session.Session;
import com.nanobot.tools.*;
import dev.langchain4j.agent.tool.*;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class AgentLoop {
    private static final Logger logger = LoggerFactory.getLogger(AgentLoop.class);
    private static final int TOOL_RESULT_MAX_CHARS = 500;

    private final MessageBus bus;
    private final ChatModel model;
    private final AppConfig config;
    private final Path workspace;
    private final int maxIterations;
    private final double temperature;
    private final int maxTokens;
    private final int memoryWindow;
    private final SessionManager sessions;
    private final ContextBuilder contextBuilder;
    private final ToolExecutor tools;

    private volatile boolean running = false;

    public AgentLoop(MessageBus bus, ChatModel model, AppConfig config) {
        this.bus = bus;
        this.model = model;
        this.config = config;

        this.workspace = expandTilde(config.getWorkspace());
        this.maxIterations = config.getMaxToolIterations();
        this.temperature = config.getTemperature();
        this.maxTokens = config.getMaxTokens();
        this.memoryWindow = config.getMemoryWindow();

        this.sessions = new SessionManager(workspace);
        this.contextBuilder = new ContextBuilder(workspace);
        this.tools = new ToolExecutor(bus, workspace, config.getRestrictToWorkspace());
    }

    public void run() {
        running = true;
        logger.info("Agent loop started");

        while (running) {
            try {
                InboundMessage msg = bus.consumeInbound();
                processMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in agent loop", e);
            }
        }
    }

    public void stop() { running = false; }

    private void processMessage(InboundMessage msg) {
        String preview = msg.content().length() > 80 ? msg.content().substring(0, 80) + "..." : msg.content();
        logger.info("Processing message from {}:{}: {}", msg.channel(), msg.senderId(), preview);

        if ("/stop".equalsIgnoreCase(msg.content().trim())) {
            handleStop(msg);
            return;
        }
        if ("/new".equalsIgnoreCase(msg.content().trim())) {
            handleNew(msg);
            return;
        }
        if ("/help".equalsIgnoreCase(msg.content().trim())) {
            sendResponse(msg, "nanobot commands:\n/new — Start a new conversation\n/stop — Stop the current task\n/help — Show available commands");
            return;
        }

        try {
            Session session = sessions.getOrCreate(msg.sessionKey());

            List<Map<String, Object>> history = new ArrayList<>();
            for (var m : session.getHistory(memoryWindow)) {
                history.add(m);
            }

            List<ChatMessage> messages = contextBuilder.buildMessages(
                    history, msg.content(), msg.channel(), msg.chatId()
            );

            String response = runAgentLoop(messages);

            if (response == null || response.isEmpty()) {
                response = "I've completed processing but have no response to give.";
            }

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", msg.content());
            userMsg.put("timestamp", java.time.Instant.now().toString());
            session.messages.add(userMsg);

            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", response);
            assistantMsg.put("timestamp", java.time.Instant.now().toString());
            session.messages.add(assistantMsg);

            sessions.save(session);

            if (!response.isEmpty()) {
                sendResponse(msg, response);
            }

        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendResponse(msg, "Sorry, I encountered an error.");
        }
    }

    private String runAgentLoop(List<ChatMessage> messages) {
        for (int i = 0; i < maxIterations; i++) {
            logger.info("Iteration {}", i + 1);

            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(tools.getToolSpecifications())
                    .build();

            ChatResponse chatResponse = model.chat(request);
            AiMessage aiMsg = chatResponse.aiMessage();

            if (aiMsg.hasToolExecutionRequests()) {
                for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                    logger.info("Tool call: {} args: {}", req.name(), req.arguments());

                    // 工具调用失败重试（最多2次）
                    String result = null;
                    for (int retry = 0; retry < 2; retry++) {
                        result = tools.execute(req.name(), req.arguments());
                        if (!result.startsWith("Error executing") && !result.contains("not found")) {
                            break;
                        }
                        if (retry < 1) {
                            logger.info("Retrying tool {} (attempt {})", req.name(), retry + 2);
                        }
                    }

                    logger.info("Tool result: {}", result);
                    // 增加工具执行成功的日志
                    if (result.contains("success") || result.contains("Error")) {
                        logger.info("Tool {} -> {}", req.name(), result.contains("success") ? "SUCCESS" : "FAILED");
                    }
                    // 防止空结果导致错误
                    if (result == null || result.isEmpty()) {
                        result = "(command executed with no output)";
                    }
                    if (result.length() > TOOL_RESULT_MAX_CHARS) {
                        result = result.substring(0, TOOL_RESULT_MAX_CHARS) + "\n... (truncated)";
                    }
                    messages.add(aiMsg);
                    messages.add(ToolExecutionResultMessage.from(req, result));
                }
            } else {
                String text = aiMsg.text();
                if (text != null && !text.isEmpty()) {
                    messages.add(aiMsg);
                    return text;
                }
            }
        }

        logger.warn("Max iterations ({}) reached", maxIterations);
        return "I reached the maximum number of tool call iterations (" + maxIterations + ") without completing the task.";
    }

    private void sendResponse(InboundMessage msg, String content) {
        try {
            bus.publishOutbound(new OutboundMessage(msg.channel(), msg.chatId(), content));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleStop(InboundMessage msg) {
        sendResponse(msg, "Stopped.");
    }

    private void handleNew(InboundMessage msg) {
        Session session = sessions.getOrCreate(msg.sessionKey());
        session.clear();
        sessions.save(session);
        sendResponse(msg, "New session started.");
    }

    // 展开 ~ 为用户主目录
    private static Path expandTilde(String path) {
        if (path == null || path.isEmpty()) {
            return Path.of(System.getProperty("user.home"));
        }
        if (path.equals("~")) {
            return Path.of(System.getProperty("user.home"));
        }
        if (path.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), path.substring(2));
        }
        return Path.of(path).toAbsolutePath();
    }
}
