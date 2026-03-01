package org.allivilsey.youmuchan;

import com.google.gson.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiProcessor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final String apiKey;
    private final String apiUrl;
    private final boolean debugMode;
    private final Logger logger;
    private final ProxyServer proxyServer;

    public ApiProcessor(String apiKey, String apiUrl, boolean debugMode, Logger logger, ProxyServer proxyServer) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.debugMode = debugMode;
        this.logger = logger;
        this.proxyServer = proxyServer;
    }

    // 发送用户消息，返回AI回复文本
    public String sendToApi(AIContext context) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("model", context.getModel());

        JsonArray messages = new JsonArray();

        // 设置系统提示词
        if (context.getSystemPrompt() != null) {
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", context.getSystemPrompt());
            messages.add(system);
        }

        // 设置提示词
        if (context.getUserPrompt() != null) {
            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", context.getUserPrompt());
            messages.add(user);
        }
        root.add("messages", messages);

        // 结构化response_format
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        root.add("response_format", responseFormat);

        // 关闭CoT
        root.addProperty("enable_thinking", false);

        // 设置对话温度
        if (context.getTemperature() != null) {
            root.addProperty("temperature", context.getTemperature());
        }

        if (debugMode) {
            Component formattedRequest = DebugFormatter.formatRequest(root.toString());
            proxyServer.getConsoleCommandSource().sendMessage(
                    Component.text("[debug_mode]", NamedTextColor.YELLOW)
                            .append(Component.text("[send]\n", NamedTextColor.GREEN))
                            .append(formattedRequest));
        }

        // 将JSON字符串包装为请求
        RequestBody body = RequestBody.create(root.toString(), JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        // 发送并接收响应
        try (Response response = CLIENT.newCall(request).execute()) {
            ResponseBody rawBody = response.body();
            String responseBody = rawBody == null ? "" : rawBody.string();

            String parsedResult = AIYoumuResultParser.parseReplay(responseBody);
            long elapsedTime = System.currentTimeMillis() - context.getRequestTime();

            if (debugMode) {
                try {
                    Component formattedResponse = DebugFormatter.formatResponse(responseBody, elapsedTime);
                    proxyServer.getConsoleCommandSource().sendMessage(
                            Component.text("[debug_mode]", NamedTextColor.YELLOW)
                                    .append(Component.text("[recv]\n", NamedTextColor.AQUA))
                                    .append(formattedResponse));
                } catch (Exception e) {
                    // 如果不是合法 JSON，就直接打印原始字符串
                    logger.info("[debug_mode][recv] {} (time: {}ms)", responseBody, elapsedTime);
                }
            }

            if (!response.isSuccessful()) {
                throw new IOException("API错误：" + response.code() + " / " + responseBody);
            }

            return parsedResult;
        }
    }
}
