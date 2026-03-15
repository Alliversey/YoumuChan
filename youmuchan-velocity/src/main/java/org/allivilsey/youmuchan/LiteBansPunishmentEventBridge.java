package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;
import litebans.api.Database;
import litebans.api.Entry;
import litebans.api.Events;
import litebans.api.PlayerProvider;
import litebans.api.exception.MissingImplementationException;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

// 将 LiteBans API 事件桥接为 Velocity 事件，方便统一使用 @Subscribe
public class LiteBansPunishmentEventBridge {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Events.Listener listener;
    private volatile String historyTableName;
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
        String punishedPlayerName = resolvePlayerName(punishedPlayerUuid);
        String executorName = resolveDisplayName(trimToNull(entry.getExecutorName()), executorUuid);

        LiteBansPunishmentEvent event = new LiteBansPunishmentEvent(
                entry.getType(),
                punishedPlayerName,
                trimToNull(entry.getReason()),
                executorName,
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

        UUID uuid = parseUuid(uuidText);
        if (uuid != null) {
            String onlinePlayerName = proxyServer.getPlayer(uuid)
                    .map(player -> sanitizeResolvedName(player.getUsername()))
                    .orElse(null);
            if (onlinePlayerName != null) {
                return onlinePlayerName;
            }
        }

        String historyPlayerName = resolvePlayerNameFromHistory(uuidText);
        if (historyPlayerName != null) {
            return historyPlayerName;
        }

        try {
            String providedName = sanitizeResolvedName(PlayerProvider.get().provide(uuidText));
            if (providedName != null) {
                return providedName;
            }

            if (uuid != null) {
                String canonicalUuid = uuid.toString();
                if (!canonicalUuid.equalsIgnoreCase(uuidText)) {
                    providedName = sanitizeResolvedName(PlayerProvider.get().provide(canonicalUuid));
                    if (providedName != null) {
                        return providedName;
                    }
                }
            }
        } catch (MissingImplementationException ignored) {
        }

        return null;
    }

    private String resolvePlayerNameFromHistory(String uuidText) {
        String historyTable = findHistoryTableName();
        if (historyTable == null) {
            return null;
        }

        try {
            String historyPlayerName = queryPlayerName(historyTable, uuidText);
            if (historyPlayerName != null) {
                return historyPlayerName;
            }

            UUID uuid = parseUuid(uuidText);
            if (uuid != null) {
                String canonicalUuid = uuid.toString();
                if (!canonicalUuid.equalsIgnoreCase(uuidText)) {
                    return queryPlayerName(historyTable, canonicalUuid);
                }
            }
        } catch (SQLException | MissingImplementationException ex) {
            logger.debug("通过 LiteBans 历史记录查询玩家名失败", ex);
        }

        return null;
    }

    private String queryPlayerName(String historyTable, String uuidText) throws SQLException {
        try (PreparedStatement statement = Database.get().prepareStatement(
                "SELECT name FROM " + historyTable + " WHERE uuid = ? ORDER BY id DESC LIMIT 1")) {
            statement.setString(1, uuidText);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return sanitizeResolvedName(resultSet.getString("name"));
            }
        }
    }

    private String findHistoryTableName() {
        String cachedTableName = historyTableName;
        if (cachedTableName != null) {
            return cachedTableName;
        }

        try (PreparedStatement statement = Database.get().prepareStatement("SELECT 1")) {
            Connection connection = statement.getConnection();
            try (ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = trimToNull(tables.getString("TABLE_NAME"));
                    if (tableName == null || !tableName.toLowerCase(Locale.ROOT).endsWith("history")) {
                        continue;
                    }
                    if (!isLiteBansHistoryTable(connection, tableName)) {
                        continue;
                    }

                    historyTableName = tableName;
                    return tableName;
                }
            }
        } catch (SQLException | MissingImplementationException ex) {
            logger.debug("定位 LiteBans 历史表失败", ex);
        }

        return null;
    }

    private boolean isLiteBansHistoryTable(Connection connection, String tableName) throws SQLException {
        return hasColumn(connection, tableName, "id")
                && hasColumn(connection, tableName, "uuid")
                && hasColumn(connection, tableName, "name");
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private String resolveDisplayName(String providedName, String uuidText) {
        String sanitizedName = sanitizeResolvedName(providedName);
        if (sanitizedName != null) {
            return sanitizedName;
        }
        return resolvePlayerName(uuidText);
    }

    private String sanitizeResolvedName(String name) {
        String trimmedName = trimToNull(name);
        if (trimmedName == null) {
            return null;
        }
        if (looksLikeUuid(trimmedName)) {
            return null;
        }
        return trimmedName;
    }

    private boolean looksLikeUuid(String value) {
        return parseUuid(value) != null;
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
