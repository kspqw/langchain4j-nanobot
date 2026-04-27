package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;

public class CronTool {
    @Tool("Schedule a task to run at a specific time or interval")
    public String schedule(@P("Cron expression or natural language time (e.g. 'tomorrow at 9am')") String schedule,
                          @P("Task description") String task) {
        return "Scheduled: " + task + " at " + schedule + " (cron support: TODO - use external cron service)";
    }
}