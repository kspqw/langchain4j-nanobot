# 控制台输入界面 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 langchain4j-nanobot 项目添加控制台输入界面，允许用户在控制台输入消息并与 Agent 交互

**Architecture:** 复用现有的 Channel 框架，创建一个 ConsoleChannel 类，使用 java.util.Scanner 读取控制台输入，解析 @nanobot 前缀，将消息发送到 MessageBus.inbound，并从 MessageBus.outbound 读取响应打印到控制台

**Tech Stack:** Java 17, java.util.Scanner, BlockingQueue

---

### Task 1: 创建 ConsoleChannelConfig 配置类

**Files:**
- Create: `D:/originate-project/langchain4j-nanobot/src/main/java/com/nanobot/config/ConsoleChannelConfig.java`

- [ ] **Step 1: 创建 ConsoleChannelConfig**

```java
package com.nanobot.config;

import java.util.List;

public record ConsoleChannelConfig(
        boolean enabled,
        String botName,
        List<String> allowFrom
) {
    public ConsoleChannelConfig() {
        this(true, "nanobot", List.of("*"));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/nanobot/config/ConsoleChannelConfig.java
git commit -m "feat(console): add ConsoleChannelConfig"
```

---

### Task 2: 创建 ConsoleChannel 实现类

**Files:**
- Create: `D:/originate-project/langchain4j-nanobot/src/main/java/com/nanobot/channels/ConsoleChannel.java`

- [ ] **Step 1: 创建 ConsoleChannel 类**

```java
package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.ConsoleChannelConfig;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class ConsoleChannel extends BaseChannel {
    private final ConsoleChannelConfig config;
    private final Scanner scanner;
    private Thread inputThread;

    public ConsoleChannel(ConsoleChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.config = config;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public String name() { return "console"; }

    @Override
    public void start() {
        running = true;
        inputThread = new Thread(this::readInput);
        inputThread.setName("console-input");
        inputThread.start();
        System.out.println("Console channel started. Type '@" + config.botName() + " <message>' to chat.");
    }

    private void readInput() {
        while (running) {
            try {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                
                // 解析 @nanobot 前缀
                String prefix = "@" + config.botName() + " ";
                if (line.startsWith(prefix)) {
                    String message = line.substring(prefix.length()).trim();
                    if (!message.isEmpty()) {
                        publishInbound("console", "console-user", "console", message, null, null);
                    }
                } else if (line.startsWith("@" + config.botName())) {
                    // 处理无空格的情况如 @nanobot
                    String message = line.substring(("@" + config.botName()).length()).trim();
                    if (!message.isEmpty()) {
                        publishInbound("console", "console-user", "console", message, null, null);
                    }
                }
            } catch (NoSuchElementException e) {
                // Ctrl+D 或 Ctrl+C 退出
                break;
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        if (scanner != null) {
            scanner.close();
        }
    }

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("\n[" + msg.chatId() + "] " + msg.content());
        System.out.print("> ");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/nanobot/channels/ConsoleChannel.java
git commit -m "feat(console): add ConsoleChannel for console input"
```

---

### Task 3: 在 AppConfig 中添加 Console 配置支持

**Files:**
- Modify: `D:/originate-project/langchain4j-nanobot/src/main/java/com/nanobot/config/AppConfig.java`
- Modify: `D:/originate-project/langchain4j-nanobot/src/main/java/com/nanobot/config/ConsoleChannelConfig.java` (import)

- [ ] **Step 1: 添加 ConsoleChannelConfig 导入**

在 import 语句区域添加:
```java
import com.nanobot.config.ConsoleChannelConfig;
```

- [ ] **Step 2: 在 ChannelsConfig record 中添加 console 字段**

```java
record ChannelsConfig(
        TelegramChannelConfig telegram,
        // ... 其他 existing fields ...
        ConsoleChannelConfig console,
        boolean sendProgress,
        boolean sendToolHints
) {
    public ChannelsConfig() {
        this(/* ... existing defaults ... */, new ConsoleChannelConfig(), true, false);
    }
}
```

- [ ] **Step 3: 添加 getConsoleConfig() 方法**

在 `AppConfig` 类中添加:
```java
public ConsoleChannelConfig getConsoleConfig() { return channels.console(); }
```

- [ ] **Step 4: 提交**

```bash
git commit -m "feat(console): integrate ConsoleChannel in AppConfig"
```

---

### Task 4: 在 ChannelManager 中注册 ConsoleChannel

**Files:**
- Modify: `D:/originate-project/langchain4j-nanobot/src/main/java/com/nanobot/channels/ChannelManager.java`

- [ ] **Step 1: 添加 ConsoleChannel 初始化**

在 `initChannels()` 方法中添加:
```java
// 检查并初始化 Console
if (config.isChannelEnabled("console")) {
    channels.put("console", new ConsoleChannel(config.getConsoleConfig(), bus));
    logger.info("Console channel configured");
}
```

- [ ] **Step 2: 提交**

```bash
git commit -m "feat(console): register ConsoleChannel in ChannelManager"
```

---

### Task 5: 验证编译

- [ ] **Step 1: 编译项目**

```bash
cd D:/originate-project/langchain4j-nanobot
mvn clean compile
```

- [ ] **Step 2: 测试运行**

启动应用后，在控制台输入:
```
@nanobot 你好
```

应该看到响应的消息打印出来。

---

## 执行选项

**Plan complete and saved.** 两个执行选项：

**1. Subagent-Driven (recommended)** - 我调度子代理逐任务执行，任务间审查，快速迭代

**2. Inline Execution** - 在当前会话中执行任务，批量执行带检查点

选择哪个？