package org.allivilsey.youmuchan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HanreiFormatter {

    public static String formatPlayerNameRequest(String requestId, String uuidText) {
        if (requestId == null || requestId.isBlank() || uuidText == null || uuidText.isBlank()) {
            return null;
        }

        JsonObject root = new JsonObject();
        root.addProperty("request_id", requestId);
        root.addProperty("uuid", uuidText);
        return root.toString();
    }

    public static PlayerNameResponse parsePlayerNameResponse(String payload) {
        JsonObject root = parseObject(payload);
        if (root == null) {
            return null;
        }

        String requestId = getString(root, "request_id");
        if (requestId == null || requestId.isBlank()) {
            return null;
        }

        String playerName = getString(root, "player_name");
        return new PlayerNameResponse(requestId, playerName);
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

    public record PlayerNameResponse(String requestId, String playerName) {
    }
}
