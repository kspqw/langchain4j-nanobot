package com.nanobot.channels;

import com.nanobot.bus.*;
import com.nanobot.config.ConsoleChannelConfig;
import java.io.*;
import java.util.*;

public class ConsoleChannel extends BaseChannel {
    private final ConsoleChannelConfig config;
    private final BufferedReader reader;
    private Thread inputThread;
    private volatile boolean running = false;

    public ConsoleChannel(ConsoleChannelConfig config, MessageBus bus) {
        super(config, bus);
        this.config = config;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public String name() { return "console"; }

    @Override
    public void start() {
        running = true;
        inputThread = new Thread(this::readInput, "console-input");
        inputThread.setDaemon(false);
        inputThread.start();
        System.out.println("\n[Console] Console channel started. Type '@" + config.botName() + " <message>' to chat.");
    }

    private void readInput() {
        String prompt = "> ";
        while (running) {
            try {
                System.out.print(prompt);
                System.out.flush();
                String line = reader.readLine();
                if (line == null) {
                    // EOF reached
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                String botMention = "@" + config.botName();
                if (line.startsWith(botMention + " ")) {
                    String message = line.substring(botMention.length() + 1).trim();
                    if (!message.isEmpty()) {
                        publishInbound("console", "console-user", "console", message, null, null);
                    }
                } else if (line.equals(botMention)) {
                    // Handle just @nanobot without space - wait for more input
                    System.out.print(">> ");
                    System.out.flush();
                    String secondLine = reader.readLine();
                    if (secondLine != null) {
                        String message = secondLine.trim();
                        if (!message.isEmpty()) {
                            publishInbound("console", "console-user", "console", message, null, null);
                        }
                    }
                }
                prompt = "\n> ";  // After sending a message, newline before next prompt
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Console] Input error: " + e.getMessage());
                }
                break;
            }
        }
        System.out.println("[Console] Console channel stopped.");
    }

    @Override
    public void stop() {
        running = false;
        try {
            reader.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void send(OutboundMessage msg) {
        System.out.println("\n[" + msg.chatId() + "] " + msg.content());
    }
}