package org.allivilsey.youmuchan.paper;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PlayerNameRequestBridge {

    public String buildPlayerNameResponse(String payload) {
        HanreiParser.PlayerNameRequest request = HanreiParser.parsePlayerNameRequest(payload);
        if (request == null) {
            return null;
        }

        String playerName = resolvePlayerName(request.uuidText());
        return HanreiFormatter.formatPlayerNameResponse(request.requestId(), playerName);
    }

    // 使用 uuid 回查玩家名，优先在线玩家，再尝试离线档案
    private String resolvePlayerName(String uuidText) {
        UUID uuid = parseUuid(uuidText);
        if (uuid == null) {
            return null;
        }

        org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return trimToNull(onlinePlayer.getName());
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return trimToNull(offlinePlayer.getName());
    }

    private UUID parseUuid(String value) {
        String trimmedValue = trimToNull(value);
        if (trimmedValue == null) {
            return null;
        }

        String normalized = trimmedValue.replace("-", "");
        if (normalized.length() != 32) {
            return null;
        }

        for (int i = 0; i < normalized.length(); i++) {
            if (Character.digit(normalized.charAt(i), 16) < 0) {
                return null;
            }
        }

        String canonicalUuid = normalized.substring(0, 8)
                + "-"
                + normalized.substring(8, 12)
                + "-"
                + normalized.substring(12, 16)
                + "-"
                + normalized.substring(16, 20)
                + "-"
                + normalized.substring(20, 32);
        try {
            return UUID.fromString(canonicalUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
