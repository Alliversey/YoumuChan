package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DebugInfo {

    private final ProxyServer proxyServer;
    private final Object plugin;
    private final HeatController heatController;
    private final FocusController focusController;
    private final GhostInThePlugin ghostInThePlugin;
    private final InGameInfoCollector collector;

    // 为每个玩家维护一个独立的 BossBar
    private final Map<Player, BossBar> debugBars = new ConcurrentHashMap<>();

    public DebugInfo(ProxyServer proxyServer,
            Object plugin,
            HeatController heatController,
            FocusController focusController,
            GhostInThePlugin ghostInThePlugin,
            InGameInfoCollector collector) {

        this.proxyServer = proxyServer;
        this.plugin = plugin;
        this.heatController = heatController;
        this.focusController = focusController;
        this.ghostInThePlugin = ghostInThePlugin;
        this.collector = collector;
    }

    public void start() {
        proxyServer.getScheduler().buildTask(plugin, this::update)
                .repeat(1, TimeUnit.SECONDS)
                .schedule();
    }

    public void togglePlayer(Player player) {

        if (debugBars.containsKey(player)) {
            BossBar bar = debugBars.remove(player);
            player.hideBossBar(bar);
            player.sendMessage(Component.text("已关闭 Debug 实时数据提示。", NamedTextColor.RED));
            return;
        }

        // 初始占位内容
        BossBar bar = BossBar.bossBar(
                Component.text("Debug Initializing...", NamedTextColor.AQUA),
                0.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS);

        debugBars.put(player, bar);
        player.showBossBar(bar);
        player.sendMessage(Component.text("已开启 Debug 实时数据提示。", NamedTextColor.GREEN));
    }

    private void update() {

        if (debugBars.isEmpty()) {
            return;
        }

        double heat = heatController.getHeat();
        double fuel = heatController.getFuel();

        String targetPlayer = focusController.getCurrentFocus();
        double focusScore = targetPlayer != null
                ? focusController.getFocusScore(targetPlayer)
                : 0.0;

        long lockTimeMs = focusController.getLockRemainingTime();
        long pulseTimeMs = ghostInThePlugin.getNextPulseTime();

        String targetStr = targetPlayer != null ? targetPlayer : "None";

        // 如果 heat 不是 0~1，需要手动归一化
        float progress = (float) Math.max(0.0, Math.min(1.0, heat));

        int cacheSize = collector.getInfoListSize();

        Component content = Component.text()
                .append(Component.text("Heat: " + String.format("%.3f", heat) + " | ", NamedTextColor.AQUA))
                .append(Component.text("Fuel: " + String.format("%.3f", fuel) + " | ", NamedTextColor.AQUA))
                .append(Component.text("Cache: " + cacheSize + " | ", NamedTextColor.AQUA))
                .append(Component.text("TgtPlayer: " + targetStr + " | ", NamedTextColor.AQUA))
                .append(Component.text("FocusScore: " + String.format("%.3f", focusScore) + " | ", NamedTextColor.AQUA))
                .append(Component.text("FocusRemain: " + String.format("%.2f", lockTimeMs / 1000.0) + "s | ",
                        NamedTextColor.AQUA))
                .append(Component.text("NextPulse: " + String.format("%.2f", pulseTimeMs / 1000.0) + "s",
                        NamedTextColor.AQUA))
                .build();

        for (Map.Entry<Player, BossBar> entry : debugBars.entrySet()) {
            Player player = entry.getKey();
            BossBar bar = entry.getValue();

            bar.name(content);
            bar.progress(progress);
        }
    }
}