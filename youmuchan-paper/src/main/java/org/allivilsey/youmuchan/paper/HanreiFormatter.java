package org.allivilsey.youmuchan.paper;

import com.google.gson.JsonObject;

public class HanreiFormatter {

    // 将 info 数据格式化为json
    public static String formatInfo(InGameInfo info) {
        if (info == null) {
            return null;
        }

        JsonObject root = new JsonObject();
        InfoType type = info.getInfoType();
        root.addProperty("type", type == null ? null : type.name());
        root.addProperty("player_name", info.getPlayerName());
        root.addProperty("server_name", info.getServerName());
        root.addProperty("content", info.getContent());

        return root.toString();
    }

    public static String formatFocus(FocusInfo info) {
        if (info == null) {
            return null;
        }

        JsonObject root = new JsonObject();
        root.addProperty("player_name", info.getFocusedPlayerName());
        root.addProperty("focus", info.getFocus());

        return root.toString();
    }

    public static String formatFuel(FuelInfo info) {
        if (info == null) {
            return null;
        }

        JsonObject root = new JsonObject();
        root.addProperty("fuel", info.getFuel());
        root.addProperty("player_name", info.getFuelPlayerName());

        return root.toString();
    }

    public static String formatPlayerNameResponse(String requestId, String playerName) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }

        JsonObject root = new JsonObject();
        root.addProperty("request_id", requestId);
        root.addProperty("player_name", playerName);
        return root.toString();
    }
}

