package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import java.io.*;
import java.net.http.*;
import java.net.URI;

public class WebTool {
    private final String apiKey;
    private final String proxy;
    private final HttpClient client;

    public WebTool(String apiKey, String proxy) {
        this.apiKey = apiKey;
        this.proxy = proxy;
        this.client = HttpClient.newHttpClient();
    }

    @Tool("Search the web for information")
    public String webSearch(@P("Search query") String query) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://duckduckgo.com/html/?q=" + query.replace(" ", "+")))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            // 简化：返回 HTML（实际生产环境需要解析）
            return body.length() > 5000 ? body.substring(0, 5000) + "\n...(truncated)" : body;
        } catch (Exception e) {
            return "Web search failed: " + e.getMessage();
        }
    }

    @Tool("Fetch the content of a web page")
    public String webFetch(@P("URL to fetch") String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            return body.length() > 10000 ? body.substring(0, 10000) + "\n... (truncated)" : body;
        } catch (Exception e) {
            return "Web fetch failed: " + e.getMessage();
        }
    }
}