package org.allivilsey.youmuchan.paper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HanreiParser {

    public static PlayerNameRequest parsePlayerNameRequest(String payload) {
        JsonObject root = parseObject(payload);
        if (root == null) {
            return null;
        }

        String requestId = getString(root, "request_id");
        String uuid = getString(root, "uuid");
        if (requestId == null || requestId.isBlank() || uuid == null || uuid.isBlank()) {
            return null;
        }
        return new PlayerNameRequest(requestId, uuid);
    }

    private static JsonObject parseObject(String payload) {
        try {
            JsonElement element = JsonParser.parseString(payload);
            if (element == null || !element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public record PlayerNameRequest(String requestId, String uuidText) {
    }
}
