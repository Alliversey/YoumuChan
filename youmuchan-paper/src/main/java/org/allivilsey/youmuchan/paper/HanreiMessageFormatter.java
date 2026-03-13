package org.allivilsey.youmuchan.paper;

import com.google.gson.JsonObject;

public class HanreiMessageFormatter {

    public static final String MESSAGE_TYPE = "info";

    // 将数据格式化为json
    public static String format(InGameInfo info) {
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
}
