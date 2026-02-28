package org.allivilsey.youmuchan;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FocusController {

    private static class FocusedPlayer {
        double value;          // 当前衰减后的fuel
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

    // 外部调用：增加focus
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

    // 内部逻辑
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

        if (bestPlayer == null) {
            currentFocus = null;
            return;
        }

        // 如果仍在锁定时间内，不切换
        if (now < lockUntil && currentFocus != null) {
            return;
        }

        // 如果当前没有focus，直接设置
        if (currentFocus == null) {
            currentFocus = bestPlayer;
            lockUntil = now + lockDuration;
            return;
        }

        FocusedPlayer current = ledger.get(currentFocus);

        if (current == null) {
            currentFocus = bestPlayer;
            lockUntil = now + lockDuration;
            return;
        }

        // 滞回控制：新玩家必须超过一定倍率才切换
        if (bestValue > current.value * switchThreshold) {
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