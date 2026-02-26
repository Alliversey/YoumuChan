package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;

public class HeatControllerListener {

    private final HeatController heatController;

    public HeatControllerListener(HeatController heatController) {
        this.heatController = heatController;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        heatController.addFuel(0.5);
    }

    @Subscribe
    public void playerLogin(PostLoginEvent event) {
        heatController.addFuel(1.0);
    }

    @Subscribe
    public void mentionedName(PlayerChatEvent event) {
        if (event.getMessage().contains("妖梦")) {
            heatController.addFuel(3.0);
        }
    }
}
