package org.allivilsey.youmuchan.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class HeatListener implements Listener {

    private final Hanrei hanrei;
    private final boolean isDefaultServer;

    public HeatListener(Hanrei hanrei, boolean isDefaultServer) {
        this.hanrei = hanrei;
        this.isDefaultServer = isDefaultServer;
    }

    @EventHandler
    public void onPlayerFirstLogin(PlayerLoginEvent event) {
        if (!isDefaultServer) {
            return;
        }

        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }

        String playerName = event.getPlayer().getName();

        FuelInfo info = new FuelInfo(3.0, playerName);

        hanrei.sendFuel(info);
    }

    @EventHandler
    public void onPlayerDead(PlayerDeathEvent event) {
        String playerName = event.getPlayer().getName();

        FuelInfo info = new FuelInfo(1.0, playerName);

        hanrei.sendFuel(info);
    }
}
