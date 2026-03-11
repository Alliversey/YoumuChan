package org.allivilsey.youmuchan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DebugFormatter {

    public static Component formatRequest(String jsonStr) {
        try {
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
            net.kyori.adventure.text.TextComponent.Builder builder = Component.text();

            if (root.has("model")) {
                builder.append(Component.text("model: ", NamedTextColor.GOLD))
                        .append(Component.text(root.get("model").getAsString()))
                        .append(Component.newline());
            }

            if (root.has("messages")) {
                JsonArray messages = root.getAsJsonArray("messages");
                for (JsonElement element : messages) {
                    JsonObject msg = element.getAsJsonObject();
                    if (msg.has("role") && msg.has("content")) {
                        builder.append(Component.text("role: ", NamedTextColor.GOLD))
                                .append(Component.text(msg.get("role").getAsString()))
                                .append(Component.newline());
                        builder.append(Component.text("content: ", NamedTextColor.GOLD))
                                .append(Component.text(msg.get("content").getAsString()))
                                .append(Component.newline());
                    }
                }
            }
            return builder.build();
        } catch (Exception e) {
            return Component.text("Failed to format request: " + e.getMessage(), NamedTextColor.RED);
        }
    }

    public static Component formatResponse(String jsonStr, long elapsedTime) {
        try {
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
            net.kyori.adventure.text.TextComponent.Builder builder = Component.text();

            if (root.has("model")) {
                builder.append(Component.text("model: ", NamedTextColor.GOLD))
                        .append(Component.text(root.get("model").getAsString()))
                        .append(Component.newline());
            }

            if (root.has("choices")) {
                JsonArray choices = root.getAsJsonArray("choices");
                for (JsonElement element : choices) {
                    JsonObject choice = element.getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message.has("role") && message.has("content")) {
                            builder.append(Component.text("role: ", NamedTextColor.GOLD))
                                    .append(Component.text(message.get("role").getAsString()))
                                    .append(Component.newline());
                            builder.append(Component.text("content: ", NamedTextColor.GOLD))
                                    .append(Component.text(message.get("content").getAsString()))
                                    .append(Component.newline());
                        }
                    }
                }
            }

            if (root.has("usage")) {
                JsonObject usage = root.getAsJsonObject("usage");
                builder.append(Component.text("usage: ", NamedTextColor.GOLD))
                        .append(Component.text(usage.toString()))
                        .append(Component.newline());
            }

            if (elapsedTime > 0) {
                builder.append(Component.text("time: ", NamedTextColor.GOLD))
                        .append(Component.text(elapsedTime + "ms"))
                        .append(Component.newline());
            }

            return builder.build();
        } catch (Exception e) {
            return Component.text("Failed to format response: " + e.getMessage(), NamedTextColor.RED);
        }
    }
}
