package com.nanobot.tools;

import dev.langchain4j.agent.tool.*;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.*;

public class WebTool {
    private final String apiKey;
    private final String proxy;

    public WebTool(String apiKey, String proxy) {
        this.apiKey = apiKey;
        this.proxy = proxy;
    }

    @Tool("Search the web for information")
    public String webSearch(@P("query") String query) {
        if (query == null || query.isBlank()) {
            return "Error: Empty search query";
        }

        String[] searchUrls = {
            // 百度搜索建议 API（国内可访问）
            "https://suggestion.baidu.com/su?wd=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
            // 微博热搜（国内可访问）
            "https://weibo.com/ajax/side/search?key=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
            // 备用：Bing
            "https://www.bing.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
        };

        String lastError = null;
        for (String url : searchUrls) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept", "text/html,application/json,*/*")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                String body = response.body();

                if (status == 200 && body.length() > 100) {
                    // 解析不同来源的结果
                    String results = parseSearchResults(body, url);
                    if (!results.isEmpty()) {
                        return results;
                    }
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                // 继续尝试下一个源
            }
        }

        return "Search unavailable: " + (lastError != null ? lastError : "all sources failed") +
               "\nTip: Try using visit_webpage with a specific URL instead.";
    }

    // 解析搜索结果（根据 URL 来源适配）
    private String parseSearchResults(String body, String sourceUrl) {
        StringBuilder sb = new StringBuilder();

        // 百度建议 API (返回 JSON 格式: ["关键词", [sug], [sug]])
        if (sourceUrl.contains("baidu.com/su")) {
            sb.append("Search suggestions for: ").append(sourceUrl.contains("wd=") ?
                extractParam(sourceUrl, "wd") : "query").append("\n\n");
            // 简单解析百度 JSON 建议
            Pattern p = Pattern.compile("\"([^\"]+)\"");
            Matcher m = p.matcher(body);
            int count = 0;
            while (m.find() && count < 10) {
                String term = m.group(1);
                if (term.length() > 1 && !term.contains("sug")) {
                    sb.append(++count).append(". ").append(term).append("\n");
                }
            }
            if (sb.length() > 0) {
                sb.insert(0, "【百度搜索建议】\n");
                return sb.toString();
            }
        }

        // 微博搜索结果
        if (sourceUrl.contains("weibo.com")) {
            sb.append("【微博搜索结果】\n\n");
            // 微博返回 JSON，提取标题
            Pattern p = Pattern.compile("\"title\":\"([^\"]+)\"");
            Matcher m = p.matcher(body);
            int count = 0;
            while (m.find() && count < 10) {
                sb.append(++count).append(". ").append(m.group(1)).append("\n");
            }
            if (sb.length() > 20) {
                return sb.toString();
            }
        }

        // Bing/通用 HTML 解析
        int count = 0;
        Pattern[] patterns = {
            Pattern.compile("<a[^>]+href=\"([^\"]+)\"[^>]*>(?:<[^>]*>)*([^<]+)</a>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("class=\"tit\"[^>]*>(?:<[^>]*>)*([^<]+)</", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pat : patterns) {
            Matcher m = pat.matcher(body);
            while (m.find() && count < 10) {
                String title = m.group(2 != m.groupCount() ? 2 : 1).trim();
                if (title.length() > 5 && !title.contains("<") && !title.toLowerCase().contains("image")) {
                    sb.append(++count).append(". ").append(title).append("\n");
                }
            }
            if (count > 0) break;
        }

        if (sb.length() == 0) {
            // 备用：提取所有链接文本
            Pattern p = Pattern.compile(">([^\n<>]{5,80})<");
            Matcher m = p.matcher(body);
            while (m.find() && count < 10) {
                String text = m.group(1).replaceAll("<[^>]+>", "").trim();
                if (text.length() > 8 && !text.toLowerCase().contains("img")) {
                    sb.append(++count).append(". ").append(text).append("\n");
                }
            }
        }

        return sb.toString();
    }

    // 从 URL 中提取参数
    private String extractParam(String url, String param) {
        Pattern p = Pattern.compile(param + "=([^&]+)");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : "unknown";
    }

    @Tool("Fetch the content of a web page")
    public String visitWebpage(@P("url") String url) {
        if (url == null || url.isBlank()) {
            return "Error: Empty URL";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (compatible; Nanobot/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            return body.length() > 5000 ? body.substring(0, 5000) + "\n... (truncated)" : body;

        } catch (Exception e) {
            return "Error fetching webpage: " + e.getMessage();
        }
    }
}