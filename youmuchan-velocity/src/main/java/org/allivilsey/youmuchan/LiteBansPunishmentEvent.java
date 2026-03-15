package org.allivilsey.youmuchan;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

// LiteBans 惩罚事件的统一封装，便于按 Velocity 监听器风格读取字段
public class LiteBansPunishmentEvent {

    private final String punishmentType;
    private final String punishedPlayerName;
    private final String reason;
    private final String executorName;
    private final Instant expireTime;
    private final boolean removed;

    public LiteBansPunishmentEvent(
            String punishmentType,
            String punishedPlayerName,
            String reason,
            String executorName,
            Instant expireTime,
            boolean removed) {
        this.punishmentType = normalizeType(punishmentType);
        this.punishedPlayerName = punishedPlayerName;
        this.reason = reason == null || reason.isBlank() ? "未提供原因" : reason;
        this.executorName = executorName;
        this.expireTime = expireTime;
        this.removed = removed;
    }

    /**
     * 获取被惩罚玩家名称。
     *
     * @return 可能为 null（例如名称无法解析）
     */
    @Nullable
    public String getPunishedPlayerName() {
        return punishedPlayerName;
    }

    /**
     * 获取执行惩罚的管理员名称。
     *
     * @return 可能为 null（例如名称不可用）
     */
    @Nullable
    public String getExecutorName() {
        return executorName;
    }

    public boolean isRemoved() {
        return removed;
    }

    /**
     * 获取完整惩罚消息。
     */
    public String getPunishmentInfo() {
        String player = valueOrDefault(punishedPlayerName, "玩家");
        String operator = valueOrDefault(executorName, "控制台");
        String action = resolveActionText(punishmentType, removed);
        String durationText = removed ? "" : resolveDurationText(expireTime);
        return player + "由于" + reason + "被" + operator + action + durationText;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "UNKNOWN";
        }
        return type.toUpperCase(Locale.ROOT);
    }

    private String resolveActionText(String type, boolean removed) {
        if (removed) {
            return switch (type) {
                case "BAN", "IPBAN", "TEMPBAN" -> "解除封禁";
                case "MUTE", "TEMPMUTE" -> "解除禁言";
                case "WARN" -> "撤销警告";
                default -> "解除惩罚";
            };
        } else {
            return switch (type) {
                case "BAN", "IPBAN", "TEMPBAN" -> "封禁";
                case "MUTE", "TEMPMUTE" -> "禁言";
                case "KICK" -> "踢出";
                case "WARN" -> "警告";
                default -> "惩罚";
            };
        }
    }

    private String resolveDurationText(@Nullable Instant endTime) {
        if (endTime == null) {
            return "，时长为永久";
        }

        Duration remaining = Duration.between(Instant.now(), endTime);
        if (remaining.isNegative() || remaining.isZero()) {
            return "，时长已到期";
        }

        long totalSeconds = remaining.getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder text = new StringBuilder("，剩余时长");
        if (days > 0) {
            text.append(days).append("天");
        }
        if (hours > 0) {
            text.append(hours).append("小时");
        }
        if (minutes > 0) {
            text.append(minutes).append("分钟");
        }
        if (seconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
            text.append(seconds).append("秒");
        }
        return text.toString();
    }

    private String valueOrDefault(@Nullable String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}