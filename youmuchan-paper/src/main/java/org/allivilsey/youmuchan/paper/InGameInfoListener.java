package org.allivilsey.youmuchan.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class InGameInfoListener implements Listener {

    private final Hanrei hanrei;
    private final boolean isDefaultServer;
    private final String serverName;

    public InGameInfoListener(Hanrei hanrei, boolean isDefaultServer, String serverName) {
        this.hanrei = hanrei;
        this.isDefaultServer = isDefaultServer;
        this.serverName = serverName;
    }

    // 玩家首次登入
    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        // 仅收集主服首次登入消息
        if (!isDefaultServer) {
            return;
        }

        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }

        String playerName = event.getPlayer().getName();
        InGameInfo info = new InGameInfo(
                InfoType.PLAYER_EVENT,
                playerName,
                null,
                "首次登入"
        );

        hanrei.sendInGameInfo(info);
    }

    // 玩家死亡
    @EventHandler
    public void onPlayerDead(PlayerDeathEvent event) {
        String playerName = event.getEntity().getName();

        Component deathComponent = event.deathMessage();
        String content = null;
        if (deathComponent != null) {
            content = PlainTextComponentSerializer.plainText().serialize(deathComponent);
        }

        if (content != null && content.isBlank()) {
            content = "死亡";
        }

        InGameInfo info = new InGameInfo(
                InfoType.PLAYER_EVENT,
                playerName,
                serverName,
                content
        );

        hanrei.sendInGameInfo(info);
    }
}
