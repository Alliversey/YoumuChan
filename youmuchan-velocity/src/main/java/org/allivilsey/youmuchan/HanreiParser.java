package org.allivilsey.youmuchan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

public class HanreiParser {

    private final InGameInfoCollector collector;
    private final HeatController heatController;
    private final FocusController focusController;

    public HanreiParser(InGameInfoCollector collector, HeatController heatController, FocusController focusController) {
        this.collector = collector;
        this.heatController = heatController;
        this.focusController = focusController;
    }

    public void parseInfo(String payload) {
        if (collector == null || payload == null || payload.isBlank()) {
            return;
        }

        JsonObject root = parseObject(payload);
        if (root == null) {
            return;
        }

        String infoTypeRaw = getString(root, "type");
        if (infoTypeRaw == null || infoTypeRaw.isBlank()) {
            return;
        }

        InfoType infoType;
        try {
            infoType = InfoType.valueOf(infoTypeRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }

        String playerName = getString(root, "player_name");
        String serverName = getString(root, "server_name");
        String content = getString(root, "content");

        InGameInfo info = new InGameInfo(infoType, playerName, serverName, content);
        collector.addInfo(info);
    }

    public void parseFocus(String payload) {
        if (collector == null || payload == null || payload.isBlank()) {
            return;
        }

        JsonObject root = parseObject(payload);

        if (root == null) {
            return;
        }

        String playerName = getString(root, "player_name");
        double focus = Double.parseDouble(getString(root, "focus"));

        focusController.addFocus(playerName, focus);
    }

    public void parseFuel(String payload) {
        if (collector == null || payload == null || payload.isBlank()) {
            return;
        }

        JsonObject root = parseObject(payload);

        if (root == null) {
            return;
        }

        double fuel = Double.parseDouble(getString(root, "fuel"));
        String playerName = getString(root, "player_name");

        heatController.addFuel(fuel, playerName);
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

