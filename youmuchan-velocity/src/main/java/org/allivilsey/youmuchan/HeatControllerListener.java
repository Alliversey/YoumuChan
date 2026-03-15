package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;

// 将游戏事件映射为热度燃料增量，驱动 AI 调度频率变化
public class HeatControllerListener {

    private final HeatController heatController;
    private final FocusController focusController;

    public HeatControllerListener(HeatController heatController, FocusController focusController) {
        this.heatController = heatController;
        this.focusController = focusController;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        // 常规聊天对 heat 的基础增益
        String player = event.getPlayer().getUsername();
        // 专注玩家可以获得更高的 fuel 增量
        if (player.equalsIgnoreCase(focusController.getCurrentFocus())) {
            heatController.addFuel(2.0, player);
        } else {
            heatController.addFuel(1.0, player);
        }
    }

    @Subscribe
    public void playerLogin(PostLoginEvent event) {
        // 玩家登录时显著加速聊天，有概率直接生成发言
        String player = event.getPlayer().getUsername();
        heatController.addFuel(2.0, player);
    }

    @Subscribe
    public void mentionedName(PlayerChatEvent event) {

        if (heatController.getFuel() > 4.0) {
            return;
        }
        // 点名“妖梦”视为高优先交互信号，显著提升热度
        if (event.getMessage().contains("妖梦")) {
            String player = event.getPlayer().getUsername();
            heatController.addFuel(3.0, player);
        }
    }

    @Subscribe
    public void onPlayerPunished(LiteBansPunishmentEvent event) {

        String player = event.getExecutorName();
        heatController.addFuel(3.0, player);
    }
}

