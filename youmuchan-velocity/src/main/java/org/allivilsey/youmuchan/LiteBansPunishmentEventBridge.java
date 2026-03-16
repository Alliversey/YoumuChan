package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import litebans.api.Entry;
import litebans.api.Events;
import litebans.api.exception.MissingImplementationException;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.UUID;

// 将 LiteBans API 事件桥接为 Velocity 事件，方便统一使用 @Subscribe
public class LiteBansPunishmentEventBridge {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Hanrei hanrei;
    private final Events.Listener listener;
    private boolean registered;

    public LiteBansPunishmentEventBridge(ProxyServer proxyServer, Logger logger, Hanrei hanrei) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.hanrei = hanrei;
        this.listener = new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                firePunishmentEvent(entry, false);
            }

            @Override
            public void entryRemoved(Entry entry) {
                firePunishmentEvent(entry, true);
            }
        };
    }

    public void register() {
        if (registered) {
            return;
        }

        try {
            Events.get().register(listener);
            registered = true;
            logger.info("LiteBans 事件桥接已启用");
        } catch (MissingImplementationException ex) {
            logger.info("未检测到 LiteBans，跳过惩罚事件桥接");
        } catch (Throwable throwable) {
            logger.error("注册 LiteBans 事件桥接失败", throwable);
        }
    }

    public void unregister() {
        if (!registered) {
            return;
        }

        try {
            Events.get().unregister(listener);
        } catch (Throwable throwable) {
            logger.error("注销 LiteBans 事件桥接失败", throwable);
        } finally {
            registered = false;
        }
    }

    private void firePunishmentEvent(Entry entry, boolean removed) {
        String punishedPlayerUuid = trimToNull(entry.getUuid());
        String operatorUuid = removed ? trimToNull(entry.getRemovedByUUID()) : trimToNull(entry.getExecutorUUID());
        Instant expireTime = resolveExpireTime(entry, removed);
        String punishedPlayerName = resolvePlayerName(punishedPlayerUuid);
        String executorName = resolvePlayerName(operatorUuid);

        LiteBansPunishmentEvent event = new LiteBansPunishmentEvent(
                entry.getType(),
                punishedPlayerName,
                trimToNull(entry.getReason()),
                executorName,
                expireTime,
                removed);

        proxyServer.getEventManager().fire(event);
    }

    private Instant resolveExpireTime(Entry entry, boolean removed) {
        if (removed || entry.isPermanent()) {
            return null;
        }

        long dateEnd = entry.getDateEnd();
        if (dateEnd <= 0L) {
            return null;
        }
        return Instant.ofEpochMilli(dateEnd);
    }

    // 通过 uuid 向 Velocity 查询在线玩家名
    private String resolvePlayerName(String uuidText) {
        UUID uuid = parseUuid(uuidText);
        if (uuid == null) {
            return null;
        }

        String playerName = proxyServer.getPlayer(uuid)
                .map(Player::getUsername)
                .orElse(null);

        if (playerName != null) {
            return playerName;
        } else {
            return trimToNull(hanrei.sendPlayerNameRequest(uuid.toString()));
        }
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
