package org.allivilsey.youmuchan;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FocusController {

    private static class FocusedPlayer {
        double value;          // 当前衰减后的 fuel
        long lastUpdate;       // 上次更新时间

        FocusedPlayer(double value, long lastUpdate) {
            this.value = value;
            this.lastUpdate = lastUpdate;
        }
    }

    private final Map<String, FocusedPlayer> ledger = new HashMap<>();

    private String currentFocus = null;
    private long lockUntil = 0;

    // 参数
    private final double lambda = 0.15;          // 衰减系数
    private final long lockDuration = 15000;     // 锁定时长(ms)
    private final double switchThreshold = 1.2;  // 切换阈值倍率
    private final double epsilon = 0.01;         // 清理阈值

    // 外部调用：增加 focus
    public void addFocus(String playerName, double fuel) {
        long now = System.currentTimeMillis();
        FocusedPlayer player = ledger.get(playerName);

        if (player == null) {
            player = new FocusedPlayer(0, now);
            ledger.put(playerName, player);
        }

        decayPlayer(player, now);
        player.value += fuel;
    }

    // 获取当前专注玩家
    public String getCurrentFocus() {
        updateFocus();
        return currentFocus;
    }

    // 内部逻辑：更新当前 focus
    private void updateFocus() {
        long now = System.currentTimeMillis();

        double total = 0;
        String bestPlayer = null;
        double bestValue = 0;

        Iterator<Map.Entry<String, FocusedPlayer>> it = ledger.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, FocusedPlayer> entry = it.next();
            FocusedPlayer p = entry.getValue();

            decayPlayer(p, now);

            if (p.value < epsilon) {
                it.remove();
                continue;
            }

            total += p.value;

            if (p.value > bestValue) {
                bestValue = p.value;
                bestPlayer = entry.getKey();
            }
        }

        // 没有活跃玩家
        if (bestPlayer == null) {
            currentFocus = null;
            return;
        }

        // 如果仍在锁定时间内，不切换
        if (now < lockUntil && currentFocus != null) {
            return;
        }

        // 当前没有 focus 或 ledger 丢失当前 focus
        if (currentFocus == null || !ledger.containsKey(currentFocus)) {
            currentFocus = bestPlayer;
            lockUntil = now + lockDuration;
            return;
        }

        FocusedPlayer current = ledger.get(currentFocus);

        // 计算相对占比
        double currentRatio = current.value / total;
        double bestRatio = bestValue / total;

        // 滞回控制 + 占比阈值
        // 当最佳玩家的占比明显超过当前玩家时才切换
        if (bestRatio > currentRatio * switchThreshold) {
            currentFocus = bestPlayer;
            lockUntil = now + lockDuration;
        }
    }

    private void decayPlayer(FocusedPlayer player, long now) {
        double deltaSeconds = (now - player.lastUpdate) / 1000.0;
        player.value *= Math.exp(-lambda * deltaSeconds);
        player.lastUpdate = now;
    }
}