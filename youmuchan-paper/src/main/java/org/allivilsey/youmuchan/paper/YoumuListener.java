package org.allivilsey.youmuchan.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class YoumuListener implements Listener {

    private final YoumuChanPaper plugin;
    private final VelocityMessenger velocityMessenger;

    public YoumuListener(YoumuChanPaper plugin, VelocityMessenger velocityMessenger) {
        this.plugin = plugin;
        this.velocityMessenger = velocityMessenger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 示例：玩家加入时尝试发送占位消息
        plugin.getLogger().fine("玩家加入: " + event.getPlayer().getName());
        velocityMessenger.sendPlayerPing(event.getPlayer());
    }
}
