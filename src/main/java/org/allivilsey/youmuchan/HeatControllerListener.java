package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;

// 将游戏事件映射为热度燃料增量，驱动 AI 调度频率变化。
public class HeatControllerListener {

    private final HeatController heatController;

    public HeatControllerListener(HeatController heatController) {
        this.heatController = heatController;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (event.getPlayer().toString().equalsIgnoreCase("youmuchan")) {
            return;
        }
        // 常规聊天对热度的基础增益。
        heatController.addFuel(0.5);
    }

    @Subscribe
    public void playerLogin(PostLoginEvent event) {
        // 登录事件通常意味着活跃度上升，给予更高增益。
        heatController.addFuel(1.0);
    }

    @Subscribe
    public void mentionedName(PlayerChatEvent event) {
        // 点名“妖梦”视为高优先交互信号，显著提升热度。
        if (event.getMessage().contains("妖梦")) {
            heatController.addFuel(3.0);
        }
    }
}
