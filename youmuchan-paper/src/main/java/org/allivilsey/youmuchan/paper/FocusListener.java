package org.allivilsey.youmuchan.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class FocusListener implements Listener {

    private final Hanrei hanrei;
    private final boolean isDefaultServer;

    public FocusListener(Hanrei hanrei, boolean isDefaultServer) {
        this.hanrei = hanrei;
        this.isDefaultServer = isDefaultServer;
    }

    @EventHandler
    public void onPlayerFirstJoin(PlayerLoginEvent event) {
        if (!isDefaultServer) {
            return;
        }

        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }

        String playerName = event.getPlayer().getName();

        FocusInfo info = new FocusInfo(playerName, 3.0);

        hanrei.sendFocus(info);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String playerName = event.getPlayer().getName();

        FocusInfo info = new FocusInfo(playerName, 1.0);

        hanrei.sendFocus(info);
    }
}
