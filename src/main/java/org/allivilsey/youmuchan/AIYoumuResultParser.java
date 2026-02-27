package org.allivilsey.youmuchan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

public class AIYoumuResultParser {

    // 迁移自 ApiProcessor.parseReply：从 API 响应中提取第一个 choice 的 message.content。
    public static String parseReolay(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");

        if (choices == null || choices.isEmpty()) {
            return "";
        }

        JsonObject first = choices.get(0).getAsJsonObject();
        JsonObject message = first.getAsJsonObject("message");
        if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
            return "";
        }
        return message.get("content").getAsString();
    }

    // 解析主模型返回的结构化消息：
    // 1) 若入参是完整 API 响应，先提取 message.content；
    // 2) 若 content 是 JSON（含 action/content），按 action 转成可发送文本；
    // 3) 若 content 不是 JSON，按纯文本直接返回。
    public static String applyResult(String jsonOrContent) {
        if (jsonOrContent == null) {
            return "";
        }

        String payload = jsonOrContent.strip();
        if (payload.isEmpty()) {
            return "";
        }

        try {
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
            if (root.has("choices")) {
                payload = parseReolay(payload).strip();
            }
        } catch (Exception ignored) {
            return payload;
        }

        if (payload.isEmpty()) {
            return "";
        }

        try {
            JsonObject result = JsonParser.parseString(payload).getAsJsonObject();
            String action = getStringSafe(result, "action", "chat").toLowerCase(Locale.ROOT);
            String content = getStringSafe(result, "content", "").strip();

            if (content.isEmpty()) {
                return "";
            }

            if ("command".equals(action)) {
                return content.startsWith("/") ? content : "/" + content;
            }
            return content;
        } catch (Exception ignored) {
            return payload;
        }
    }

    private static String getStringSafe(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }
}
