package org.allivilsey.youmuchan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

public class AIYoumuResultParser {

    //解析妖梦回复
    public static String parseReplay(String json) {
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

    //
    public static String applyResult(String jsonOrContent) {
        if (jsonOrContent == null) {
            return "";
        }

        String payload = jsonOrContent.strip();
        if (payload.isEmpty()) {
            return "";
        }

        JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
        if (root.has("choices")) {
            payload = parseReplay(payload).strip();
        }

        if (payload.isEmpty()) {
            return "";
        }

        JsonObject result = JsonParser.parseString(payload).getAsJsonObject();
        String action = result.get("action").getAsString().toLowerCase(Locale.ROOT);
        String content = result.get("content").getAsString().strip();

        if (content.isEmpty()) {
            return "";
        }

        if ("command".equals(action)) {
            return content.startsWith("/") ? content : "/" + content;
        }
        return content;
    }
}
