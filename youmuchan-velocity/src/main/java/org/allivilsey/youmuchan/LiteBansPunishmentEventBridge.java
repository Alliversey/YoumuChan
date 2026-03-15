package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;
import litebans.api.Entry;
import litebans.api.Events;
import litebans.api.PlayerProvider;
import litebans.api.exception.MissingImplementationException;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.UUID;

// 将 LiteBans API 事件桥接为 Velocity 事件，方便统一使用 @Subscribe
public class LiteBansPunishmentEventBridge {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Events.Listener listener;
    private boolean registered;

    public LiteBansPunishmentEventBridge(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.listener = new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                firePunishmentEvent(entry);
            }

            @Override
            public void entryRemoved(Entry entry) {
                firePunishmentEvent(entry);
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

    private void firePunishmentEvent(Entry entry) {
        String punishedPlayerUuid = trimToNull(entry.getUuid());
        String executorUuid = trimToNull(entry.getExecutorUUID());
        Instant expireTime = resolveExpireTime(entry);

        LiteBansPunishmentEvent event = new LiteBansPunishmentEvent(
                entry.getType(),
                resolvePlayerName(punishedPlayerUuid),
                trimToNull(entry.getReason()),
                firstNonBlank(trimToNull(entry.getExecutorName()), resolvePlayerName(executorUuid)),
                expireTime);

        proxyServer.getEventManager().fire(event);
    }

    private Instant resolveExpireTime(Entry entry) {
        if (entry.isPermanent()) {
            return null;
        }

        long dateEnd = entry.getDateEnd();
        if (dateEnd <= 0L) {
            return null;
        }
        return Instant.ofEpochMilli(dateEnd);
    }

    private String resolvePlayerName(String uuidText) {
        if (uuidText == null) {
            return null;
        }

        try {
            String providedName = PlayerProvider.get().provide(uuidText);
            if (providedName != null && !providedName.isBlank()) {
                return providedName;
            }
        } catch (MissingImplementationException ignored) {
        }

        try {
            UUID uuid = UUID.fromString(uuidText);
            return proxyServer.getPlayer(uuid)
                    .map(player -> player.getUsername())
                    .orElse(null);
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

    private String firstNonBlank(String left, String right) {
        if (left != null && !left.isBlank()) {
            return left;
        }
        if (right != null && !right.isBlank()) {
            return right;
        }
        return null;
    }
}