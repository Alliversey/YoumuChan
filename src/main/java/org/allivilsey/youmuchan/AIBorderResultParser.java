package org.allivilsey.youmuchan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AIBorderResultParser {

    public static void applyResult(String json, AIContext context) {

        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        context.setInjectionRisk(object.get("injection").getAsBoolean());

        context.setEmotion(object.get("emotion").getAsString());

        context.setWikiRequired(object.get("wiki").getAsBoolean());
    }
}
