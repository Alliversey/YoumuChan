package org.allivilsey.youmuchan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

public class HanreiMessageParser {

    public static final String MESSAGE_TYPE = "info";

    private final InGameInfoCollector collector;

    public HanreiMessageParser(InGameInfoCollector collector) {
        this.collector = collector;
    }

    public void parseAndCollect(String payload) {
        if (collector == null || payload == null || payload.isBlank()) {
            return;
        }

        JsonObject root = parseObject(payload);
        if (root == null) {
            return;
        }

        String typeRaw = getString(root, "type");
        if (typeRaw == null || typeRaw.isBlank()) {
            return;
        }

        InfoType type;
        try {
            type = InfoType.valueOf(typeRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }

        String playerName = getString(root, "player_name");
        String serverName = getString(root, "server_name");
        String content = getString(root, "content");

        InGameInfo info = new InGameInfo(type, playerName, serverName, content);
        collector.addInfo(info);
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
}
